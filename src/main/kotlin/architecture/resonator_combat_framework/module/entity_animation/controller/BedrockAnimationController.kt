package architecture.resonator_combat_framework.module.entity_animation.controller

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.entity_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.entity_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.data.*
import architecture.resonator_combat_framework.module.entity_animation.engine.BedrockAnimation
import architecture.resonator_combat_framework.module.entity_animation.engine.BedrockAnimator
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangQueries
import architecture.resonator_combat_framework.module.entity_animation.event.AnimationControllerEvent
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.ProxyBoneConfigRegistry
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.common.NeoForge

/** 动画控制器——状态机 + crossfade 过渡 + 权重混合 + Bedrock 后端插值 */
class BedrockAnimationController @JvmOverloads constructor(
	override val id: ResourceLocation,
	protected val isClient: Boolean,
	override val isOverriding: Boolean = true
) : IAnimationController {
	protected val animationLoader = BedrockAnimationRegistry.getInstance(isClient)

	protected val configLoader: ProxyBoneConfigRegistry = ProxyBoneConfigRegistry.getInstance(isClient)

	/** 控制器状态机 */
	protected enum class State { IDLE, TRANSITIONING, PLAYING, PAUSED, FADING_OUT }

	/** 当前游戏刻的骨骼状态 */
	val proxyModel = ProxyModel("base")

	/** 控制器状态 */
	protected var state = State.IDLE

	/** 过渡开始时的骨骼快照，用于 crossfade */
	protected var transitionSource: ProxyModel? = null

	/** 当前完整播放配置 */
	protected var currentConfig: AnimationPlayData = AnimationPlayData.EMPTY

	/** trigger 时临时覆盖，设完后立即清空 */
	internal var resolvedBoneConfig: ProxyBoneConfigData? = null

	/** 当前活跃骨骼配置（crossfade 时保留引用），trigger 时设，forceClear 时清除 */
	private var activeBoneConfig: ProxyBoneConfigData = ProxyBoneConfigData.EMPTY

	/** 额外骨骼配置（通常 null）。优先级高于 activeBoneConfig，只覆盖已存在的骨骼 */
	var boneConfigs: ProxyBoneConfigData? = null

	/** 混合因子 0~1 */
	override var blendFactor = 0f

	/** 混合目标值 */
	override var blendTarget = 0f

	/** 当前过渡 tick 数 */
	override var currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS

	/** 播放速度倍率 */
	override var speedMultiplier = 1f

	/** 当前播放动画 ID */
	override var currentAnimId: String? = null

	/** 当前帧受影响的骨骼名集合 */
	override var affectedBones = emptySet<String>()

	/** 游戏刻推进计数器，用于 tickAdvance 传参给 tickBackend */
	protected var advanceTickCount = 0L

	/** 当前加载的动画数据 */
	private var currentAnim: BedrockAnimation? = null

	/** 当前动画播放位置（秒） */
	private var animTime = 0f

	/** 上一帧的 gameTime，-1 表示首帧 */
	private var lastRawGameTime = -1f

	override val currentAnimTime: Float
		get() {
			val info = currentAnimId?.let { getPlaybackInfo() } ?: return 0f
			return info.animTime
		}

	override val effectiveWeight: Float get() = if (transitionSource != null) 1f else blendFactor

	override val isFadingOut: Boolean get() = state == State.FADING_OUT

	/** 获取当前活跃骨骼配置的骨骼标志 */
	fun resolveBoneFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val flags = activeBoneConfig.resolveBoneFlags(animTime).toMutableMap()
		val overrideFlags = boneConfigs?.resolveBoneFlags(animTime) ?: return flags
		for ((boneName, boneFlag) in overrideFlags) {
			flags[boneName] = boneFlag
		}
		return flags
	}

	override fun isActive(): Boolean = state != State.IDLE

	/** 播放信息：当前时间 / 总长度 / 循环类型 */
	data class PlaybackInfo(
		val animTime: Float,
		val animLength: Float,
		val loopType: LoopType
	)

	/** 循环类型 */
	enum class LoopType { ONCE, LOOP, HOLD_ON_LAST }

	// ---- 触发 ----

	override fun trigger(config: AnimationPlayData) {
		if (!loadAnimation(config.animId)) {
			RcfConstants.LOGGER.warn("[AnimDebug] Animation not found: " + config.animId)
			return
		}
		speedMultiplier = config.resolveSpeedMultiplier()
		val finalConfig = resolvedBoneConfig ?: config.boneConfig ?: configLoader.getConfig(config.animId)
		resolvedBoneConfig = null
		activeBoneConfig = finalConfig
		snapshotTransitionSource()
		proxyModel.bones.clear()
		currentConfig = config
		currentAnimId = config.animId
		if (config.startTime > 0)
			setAnimStartTime(config.startTime / 20f)
		currentTransitionTicks = config.resolveFadeInTicks(finalConfig.getFadeInTicks())
		state = State.TRANSITIONING
		blendFactor = 0f
		blendTarget = 1f
		freezeAllAtFrameZero()
	}

	// ---- 停止 ----

	override fun stop(fadeOutTicks: Int) {
		if (state == State.IDLE || state == State.FADING_OUT) return
		state = State.FADING_OUT
		blendTarget = 0f
		currentTransitionTicks = if (fadeOutTicks >= 0) fadeOutTicks
		else currentConfig.resolveFadeOutTicks(activeBoneConfig.getFadeOutTicks())
		if (currentTransitionTicks <= 0) forceClear()
	}

	// ---- 暂停 / 恢复 ----

	override fun pause() {
		if (state == State.PLAYING || state == State.TRANSITIONING) {
			state = State.PAUSED
			transitionSource = null
		}
	}

	override fun resume() {
		if (state != State.PAUSED) return
		val info = currentAnimId?.let { getPlaybackInfo() }
		if (info != null && info.animTime * speedMultiplier >= info.animLength
			&& info.loopType == LoopType.HOLD_ON_LAST
		) return
		state = if (isInFadeIn()) State.TRANSITIONING else State.PLAYING
	}

	// ---- 每帧 ----

	/** tick 处理钩子，子类可重写（如 ActionAnimationController 检测物品切换） */
	fun tickHandler(animationMapper: IAnimationMapper) {
	}

	// ---- crossfade 过渡 ----

	/** 快照当前骨骼为过渡源 */
	private fun snapshotTransitionSource() {
		if (state == State.IDLE || proxyModel.bones.isEmpty()) return
		transitionSource = ProxyModel("src")
		for ((name, bone) in proxyModel.bones) {
			val copy = ProxyBone(name)
			copy.pos.set(bone.pos)
			copy.rotation.set(bone.rotation)
			copy.scale.set(bone.scale)
			transitionSource!!.addBone(copy)
		}
	}

	/** 跨动画过渡混合 */
	private fun crossfadeStep() {
		val src = transitionSource ?: return
		val boneFlags = activeBoneConfig.resolveBoneFlags(currentAnimTime)
		// 旧动画独有骨骼：每帧重置为 identity
		for ((name, bone) in proxyModel.bones) {
			if (name !in affectedBones) {
				bone.pos.set(0f)
				bone.rotation.set(0f)
				bone.scale.set(1f)
			}
		}
		for ((name, _) in proxyModel.bones) {
			if (src.getBone(name) == null) src.addBone(ProxyBone(name))
		}
		for ((name, _) in src.bones) {
			if (proxyModel.getBone(name) == null) proxyModel.addBone(ProxyBone(name))
		}
		for ((name, fromBone) in src.bones) {
			val toBone = proxyModel.getBone(name) ?: continue
			if (!boneFlags[name].shouldBlend()) {
				fromBone.pos.set(toBone.pos)
				fromBone.rotation.set(toBone.rotation)
				fromBone.scale.set(toBone.scale)
				continue
			}
			fromBone.pos.lerp(toBone.pos, blendFactor, toBone.pos)
			fromBone.rotation.lerp(toBone.rotation, blendFactor, toBone.rotation)
			fromBone.scale.lerp(toBone.scale, blendFactor, toBone.scale)
		}
	}

	// ---- 播放边界检查 ----

	/** 检查动画播放边界（结束/循环/淡出） */
	private fun checkPlaybackBounds() {
		if (state != State.PLAYING) return
		val info = currentAnimId?.let { getPlaybackInfo() } ?: return
		val config = currentConfig
		val animLength = info.animLength
		val endSec = when {
			config.endTime < 0 -> animLength + config.endTime / 20f
			config.endTime > 0 -> config.endTime / 20f
			else -> animLength
		}
		if (info.animTime < endSec || endSec <= 0f) return
		when (config.animType) {
			AnimType.PLAY_ONCE -> {
				pause()
				state = State.FADING_OUT
				blendTarget = 0f
				currentTransitionTicks = currentConfig.resolveFadeOutTicks(activeBoneConfig.getFadeOutTicks())
			}

			AnimType.LOOP -> {
				resetAnimAndRestart()
				if (config.startTime > 0) setAnimStartTime(config.startTime / 20f)
			}

			AnimType.STOP_AT_LAST -> pause()
			AnimType.DEFAULT -> {
				when (info.loopType) {
					LoopType.ONCE -> {
						pause()
						state = State.FADING_OUT
						blendTarget = 0f
						currentTransitionTicks = currentConfig.resolveFadeOutTicks(activeBoneConfig.getFadeOutTicks())
					}

					LoopType.LOOP -> resetAnimAndRestart()
					LoopType.HOLD_ON_LAST -> pause()
				}
			}
		}
	}

	/** 重置动画时间并从开头重新播放 */
	fun resetAnimAndRestart() {
		animTime = 0f
	}

	// ---- tick ----

	/** 游戏刻推进（20tps）：推进动画时间、计算骨骼、检查播放边界 */
	final override fun tickAdvance(animationMapper: IAnimationMapper) {
		val pre = NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickPre(id, this, animationMapper))
		if (pre.isCanceled) return
		val preHandler = NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickHandlerPre(id, this, animationMapper))
		if (!preHandler.isCanceled) {
			tickHandler(animationMapper)
			NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickHandlerPre(id, this, animationMapper))
		}

		if (state != State.IDLE && state != State.PAUSED) {
			checkPlaybackBounds()
			advanceTickCount++
			tickBackend(advanceTickCount / 20f)
		}
		NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickPost(id, this, animationMapper))
	}

	override fun tickRender(deltaSec: Float) {
		if (state == State.IDLE) return
		tickBlend(deltaSec)
		if (affectedBones.isNotEmpty()) {
			val toRemove = proxyModel.bones.keys.filter { it !in affectedBones }.toList()
			for (name in toRemove) proxyModel.bones.remove(name)
		}
		if (state == State.PAUSED) return
		if (state == State.TRANSITIONING && transitionSource != null) crossfadeStep()
		if (state == State.TRANSITIONING && blendFactor >= 1f) {
			state = State.PLAYING
			transitionSource = null
		}
		if (state == State.FADING_OUT && blendFactor <= 0f) forceClear()
	}

	/** 加载 BedrockAnimation */
	fun loadAnimation(animId: String): Boolean {
		currentAnim = animationLoader.get(animId)
		return currentAnim != null
	}

	/** 重置动画时间到 0 */
	fun freezeAllAtFrameZero() {
		animTime = 0f
		lastRawGameTime = -1f
	}

	/** 设置动画起始时间 */
	fun setAnimStartTime(timeSec: Float) {
		animTime = timeSec
	}

	/** 获取当前播放信息 */
	fun getPlaybackInfo(): PlaybackInfo? {
		val anim = currentAnim ?: return null
		return PlaybackInfo(animTime, anim.length, anim.loop.toLoopType())
	}

	/** 驱动 MoLang anim_time_update 并写骨骼到 proxyModel */
	fun tickBackend(gameTime: Float) {
		val anim = currentAnim ?: return
		val scaledDelta: Float
		if (lastRawGameTime < 0f) {
			lastRawGameTime = gameTime
			scaledDelta = 0f
			animTime = 0f
		} else {
			val delta = gameTime - lastRawGameTime
			lastRawGameTime = gameTime
			scaledDelta = delta * speedMultiplier
		}
		val expr = anim.animTimeUpdate
		if (expr != null) {
			MolangQueries.setVariable("query.anim_time") { animTime.toDouble() }
			MolangQueries.setVariable("query.delta_time") { scaledDelta.toDouble() }
			animTime = expr.get().toFloat()
		} else {
			animTime += scaledDelta
		}
		affectedBones = BedrockAnimator.computeAndWrite(anim, animTime, proxyModel)
	}

	// ---- 内部 ----

	private fun tickBlend(ds: Float) {
		if (blendFactor == blendTarget) return
		if (currentTransitionTicks <= 0) {
			blendFactor = blendTarget
			return
		}
		val step = (ds * 20f) / currentTransitionTicks
		blendFactor = if (blendFactor < blendTarget) {
			(blendFactor + step).coerceAtMost(blendTarget)
		} else {
			(blendFactor - step).coerceAtLeast(blendTarget)
		}
	}

	private fun forceClear() {
		state = State.IDLE
		blendFactor = 0f
		blendTarget = 0f
		proxyModel.bones.clear()
		transitionSource = null
		currentConfig = AnimationPlayData.EMPTY
		currentAnimId = null
		affectedBones = emptySet()
		currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS
		speedMultiplier = 1f
		activeBoneConfig = ProxyBoneConfigData.EMPTY
	}

	private fun isInFadeIn(): Boolean = blendTarget > 0f && blendFactor < 1f

	/** 转换 Bedrock 循环类型到枚举 */
	fun BedrockAnimation.LoopType.toLoopType() = when (this) {
		BedrockAnimation.LoopType.ONCE -> LoopType.ONCE
		BedrockAnimation.LoopType.LOOP -> LoopType.LOOP
		BedrockAnimation.LoopType.HOLD_ON_LAST -> LoopType.HOLD_ON_LAST
	}
}
