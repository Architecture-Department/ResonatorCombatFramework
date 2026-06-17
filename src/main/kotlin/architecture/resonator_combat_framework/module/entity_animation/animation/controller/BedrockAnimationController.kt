package architecture.resonator_combat_framework.module.entity_animation.animation.controller

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.animation.*
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController.State
import architecture.resonator_combat_framework.module.entity_animation.animation.data.*
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapper
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BakingBrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyBone
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.event.AnimationControllerEvent
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.ProxyBoneConfigDataRegistry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.neoforged.neoforge.common.NeoForge

/** 动画控制器——状态机 + crossfade 过渡 + 权重混合 + Bedrock 后端插值 */
class BedrockAnimationController<T : Entity> @JvmOverloads constructor(
	override val manager: AnimationControllerManager<T>,
	override val id: ResourceLocation,
	protected val isClient: Boolean,
	override val isOverriding: Boolean = true
) : IEntityAnimationController<T> {
	protected val animationLoader = BedrockAnimationRegistry.getInstance(isClient)

	protected val configLoader: ProxyBoneConfigDataRegistry = ProxyBoneConfigDataRegistry.getInstance(isClient)

	/** 当前游戏刻的骨骼状态 */
	val proxyModel = ProxyModel("base")

	/** 控制器状态 */
	override var state = State.IDLE

	/** 过渡开始时的骨骼快照，用于 crossfade */
	override var transitionSource: ProxyModel? = null

	/** 当前完整播放配置 */
	override var currentConfig: AnimationPlayData = AnimationPlayData.EMPTY

	/** trigger 时临时覆盖，设完后立即清空 */
	override var resolvedBoneConfig: ProxyBoneConfigData? = null

	/** 当前活跃骨骼配置（crossfade 时保留引用），trigger 时设，forceClear 时清除 */
	override var activeBoneConfig: ProxyBoneConfigData = ProxyBoneConfigData.EMPTY

	/** 额外骨骼配置（通常 null）。优先级高于 activeBoneConfig，只覆盖已存在的骨骼 */
	override var boneConfigs: ProxyBoneConfigData? = null

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
	private var currentAnim: BakingBrAnimation? = null

	/** 已触发的事件索引集合（避免重复触发） */
	private val firedEvents = mutableSetOf<String>()

	/** 当前动画的额外模型定义 */
	var extraModel: BakingBrModel? = null

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

	override val isFadingIn: Boolean get() = state == State.TRANSITIONING

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
		firedEvents.clear()
		if (config.startTime > 0) setAnimStartTime(config.startTime / 20f)
		currentTransitionTicks = config.resolveFadeInTicks(finalConfig.getFadeInTicks())
		state = State.TRANSITIONING
		blendFactor = 0f
		blendTarget = 1f
		freezeAllAtFrameZero()
		// 立即写入第0帧骨骼，防止 trigger→下一 tick 之间的渲染帧拿到空的 proxyModel
		// 同时更新 affectedBones（crossfadeStep 依赖其判断骨骼归属）
		affectedBones = currentAnim?.computeAndWrite(animTime, proxyModel, currentData) ?: emptySet()
		// 有 crossfade 源时立即混合，使 trigger 后的首帧也保持旧动画姿态
		if (transitionSource != null) {
			crossfadeStep()
		}
		extraModel = finalConfig.extraModel
		manager.rebuildBones()
	}

	// ---- 停止 ----

	override fun stop(fadeOutTicks: Int) {
		if (state == State.IDLE || state == State.FADING_OUT) return
		state = State.FADING_OUT
		blendTarget = 0f
		transitionSource = null // 清除 crossfade 源，使 effectiveWeight = blendFactor
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
	fun tickHandler(manager: IEntityAnimationMapper<T, *>) {
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
			// 复制 emptyMask，使过渡源骨骼的 hasPos/hasRot/hasScale 正确反映数据状态
			if (bone.hasPos()) copy.setPosEmpty(false)
			if (bone.hasRot()) copy.setRotEmpty(false)
			if (bone.hasScale()) copy.setScaleEmpty(false)
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
			// lerp 写入 Vector3f 但不更新 emptyMask，需根据源/目标数据状态同步
			if (fromBone.hasPos() || toBone.hasPos()) toBone.setPosEmpty(false)
			if (fromBone.hasRot() || toBone.hasRot()) toBone.setRotEmpty(false)
			if (fromBone.hasScale() || toBone.hasScale()) toBone.setScaleEmpty(false)
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
		firedEvents.clear()
	}

	// ---- 游戏刻推进 ----

	/** 游戏刻推进（20tps）：推进动画时间、计算骨骼、检查播放边界 */
	final override fun tickAdvance() {
		val pre = NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickPre(id, this, manager.mapper))
		if (pre.isCanceled) return
		val preHandler = NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickHandlerPre(id, this, manager.mapper))
		if (!preHandler.isCanceled) {
			tickHandler(manager.mapper)
			NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickHandlerPost(id, this, manager.mapper))
		}

		if (state != State.IDLE && state != State.PAUSED) {
			checkPlaybackBounds()
			tickBlend() // 基于 tick 推进 blendFactor
			if (state == State.PLAYING) {
				advanceTickCount++
				tickBackend(advanceTickCount / 20f, manager.mapper.holder, manager.mapper.molangData)
			} else {
				// TRANSITIONING / FADING_OUT：使用当前（冻结的）animTime 计算骨骼，不推进时间
				tickBackend(0f, manager.mapper.holder, manager.mapper.molangData, freezeTime = true)
			}
			// 跨动画过渡混合（仅在有 transitionSource 时执行）
			if (state == State.TRANSITIONING && transitionSource != null) {
				crossfadeStep()
			}
			// 状态转移检查（基于 tick 推进后的 blendFactor）
			if (state == State.TRANSITIONING && blendFactor >= 1f) {
				state = State.PLAYING
				transitionSource = null
				// 初始化 lastRawGameTime，避免首个 PLAYING tick 因 lastRawGameTime=-1 而 delta=0
				lastRawGameTime = advanceTickCount / 20f
			}
			if (state == State.FADING_OUT && blendFactor <= 0f) {
				forceClear()
			}
		}
		NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickPost(id, this, manager.mapper))
	}

	override fun tickRender(deltaSec: Float) {
		// 骨骼清理仅在 PLAYING 时执行（TRANSITIONING 中旧动画骨骼由 crossfadeStep 维护）
		if (state != State.PLAYING) return
		if (affectedBones.isNotEmpty()) {
			val toRemove = proxyModel.bones.keys.filter { it !in affectedBones }.toList()
			for (name in toRemove) proxyModel.bones.remove(name)
		}
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
	fun tickBackend(gameTime: Float, entity: Entity, data: MolangData, freezeTime: Boolean = false) {
		val anim = currentAnim ?: return
		if (freezeTime) {
			// 过渡模式：不推进 animTime，只基于当前时间重新计算骨骼
			affectedBones = anim.computeAndWrite(animTime, proxyModel, data)
			val events = collectEventsAt(anim)
			manager.queueEvents(this, events)
			return
		}
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
			// 通过 MolangData 设置动画查询值并求值
			val data = currentData
			data.updateAnimQueries(animTime, scaledDelta)
			animTime = expr.eval(data).toFloat()
		} else {
			animTime += scaledDelta
		}
		affectedBones = anim.computeAndWrite(animTime, proxyModel, data)
		// 收集待触发的事件，交给管理器统一执行
		val events = collectEventsAt(anim)
		manager.queueEvents(this, events)
	}

	// ---- 内部 ----


	/** 收集当前动画时间点上待触发的音效/粒子/时间线事件 */
	fun collectEventsAt(anim: BakingBrAnimation): AnimationEventsToFire {
		val soundsToFire = mutableListOf<BakingBrAnimationSound>()
		val particlesToFire = mutableListOf<BakingBrAnimationParticle>()
		val timelinesToFire = mutableListOf<BakingBrAnimationTimeline>()

		// 独立遍历音效、粒子、时间线，互不嵌套
		anim.sounds.forEachIndexed { i, sound ->
			val key = "sound_$i"
			if (key !in firedEvents && animTime >= sound.time) {
				firedEvents.add(key)
				soundsToFire.add(sound)
			}
		}
		anim.particles.forEachIndexed { i, particle ->
			val key = "particle_$i"
			if (key !in firedEvents && animTime >= particle.time) {
				firedEvents.add(key)
				particlesToFire.add(particle)
			}
		}
		anim.timelines.forEachIndexed { i, timeline ->
			val key = "timeline_$i"
			if (key !in firedEvents && animTime >= timeline.time) {
				firedEvents.add(key)
				timelinesToFire.add(timeline)
			}
		}

		return AnimationEventsToFire(soundsToFire, particlesToFire, timelinesToFire)
	}

	private fun tickBlend() {
		// 基于 tick 推进 blendFactor：每 tick 前进 1/currentTransitionTicks
		if (blendFactor == blendTarget) return
		if (currentTransitionTicks <= 0) {
			blendFactor = blendTarget
			return
		}
		val step = 1f / currentTransitionTicks
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
		extraModel = null
		manager.rebuildBones()
	}

	private fun isInFadeIn(): Boolean = isFadingIn

	/** 转换 Bedrock 循环类型到枚举 */
	fun BakingBrAnimation.LoopType.toLoopType() = when (this) {
		BakingBrAnimation.LoopType.ONCE -> LoopType.ONCE
		BakingBrAnimation.LoopType.LOOP -> LoopType.LOOP
		BakingBrAnimation.LoopType.HOLD_ON_LAST -> LoopType.HOLD_ON_LAST
	}
}
