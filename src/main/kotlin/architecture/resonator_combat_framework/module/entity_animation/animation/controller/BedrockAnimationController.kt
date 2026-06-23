package architecture.resonator_combat_framework.module.entity_animation.animation.controller

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
import architecture.resonator_combat_framework.util.RcfUtil
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

	/** 动画数据加载器 */
	protected val animationLoader = BedrockAnimationRegistry.getInstance(isClient)

	/** 骨骼配置加载器 */
	protected val configLoader: ProxyBoneConfigDataRegistry = ProxyBoneConfigDataRegistry.getInstance(isClient)

	/** 当前游戏刻的骨骼状态 */
	val proxyModel = ProxyModel("base")

	override var state = State.IDLE

	/** 过渡开始时的骨骼快照，用于 crossfade 混合 */
	override var transitionSource: ProxyModel? = null

	override var currentConfig: AnimationPlayData = AnimationPlayData.EMPTY

	override var resolvedBoneConfig: ProxyBoneConfigData? = null

	override var activeBoneConfig: ProxyBoneConfigData = ProxyBoneConfigData.EMPTY

	override var boneConfigs: ProxyBoneConfigData? = null

	override var blendFactor = 0f
	override var blendTarget = 0f
	override var currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS
	override var speedMultiplier = 1f
	override var currentAnimId: String? = null
	override var affectedBones = emptySet<String>()

	/** 游戏刻推进计数器，用于 tickBackend 计算 delta time */
	protected var advanceTickCount = 0L

	/** 当前加载的动画数据 */
	private var currentAnim: BakingBrAnimation? = null

	/** 已触发的事件索引集合，避免重复触发 */
	private val firedEvents = mutableSetOf<String>()

	/** 当前动画的额外（骨骼）模型定义 */
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

	// ===== 骨骼标志 =====

	/** 合并控制器骨骼标志与配置覆盖标志 */
	fun resolveBoneFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val flags = activeBoneConfig.resolveBoneFlags(animTime).toMutableMap()
		val overrideFlags = boneConfigs?.resolveBoneFlags(animTime) ?: return flags
		overrideFlags.forEach { (boneName, boneFlag) -> flags[boneName] = boneFlag }
		return flags
	}

	override fun isActive(): Boolean = state != State.IDLE

	/** 播放信息 */
	data class PlaybackInfo(val animTime: Float, val animLength: Float, val loopType: LoopType)

	enum class LoopType { ONCE, LOOP, HOLD_ON_LAST }

	// ===== 触发 =====

	override fun trigger(config: AnimationPlayData) {
		manager.clearEmittersFor(id)
		if (!loadAnimation(config.animId)) {
			RcfUtil.LOGGER.warn("[AnimDebug] Animation not found: " + config.animId)
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
		affectedBones = currentAnim?.computeAndWrite(animTime, proxyModel, currentData) ?: emptySet()
		// 有 crossfade 源时立即混合，使 trigger 后的首帧也保持旧动画姿态
		if (transitionSource != null) crossfadeStep()
		extraModel = finalConfig.extraModel
		manager.rebuildBones()
	}

	// ===== 停止 =====

	override fun stop(fadeOutTicks: Int) {
		if (state == State.IDLE || state == State.FADING_OUT) return
		manager.clearEmittersFor(id)
		state = State.FADING_OUT
		blendTarget = 0f
		transitionSource = null // 清除 crossfade 源，使 effectiveWeight = blendFactor
		currentTransitionTicks = if (fadeOutTicks >= 0) fadeOutTicks
		else currentConfig.resolveFadeOutTicks(activeBoneConfig.getFadeOutTicks())
		if (currentTransitionTicks <= 0) forceClear()
	}

	// ===== 暂停 / 恢复 =====

	override fun pause() {
		if (state == State.PLAYING || state == State.TRANSITIONING) {
			state = State.PAUSED
			transitionSource = null
		}
	}

	override fun resume() {
		if (state != State.PAUSED) return
		// HOLD_ON_LAST 且播完则不允许恢复
		val info = currentAnimId?.let { getPlaybackInfo() }
		if (info != null && info.animTime * speedMultiplier >= info.animLength
			&& info.loopType == LoopType.HOLD_ON_LAST
		) return
		state = if (isInFadeIn()) State.TRANSITIONING else State.PLAYING
	}

	/** tick 处理钩子，子类可重写（如 ActionAnimationController 检测物品切换） */
	fun tickHandler(manager: IEntityAnimationMapper<T, *>) {}

	// ===== crossfade 过渡 =====

	/** 快照当前骨骼为过渡源 */
	private fun snapshotTransitionSource() {
		if (state == State.IDLE || proxyModel.bones.isEmpty()) return
		transitionSource = ProxyModel("src")
		for ((name, bone) in proxyModel.bones) {
			val copy = ProxyBone(name)
			copy.pos.set(bone.pos)
			copy.rotation.set(bone.rotation)
			copy.scale.set(bone.scale)
			if (bone.hasPos()) copy.setPosEmpty(false)
			if (bone.hasRot()) copy.setRotEmpty(false)
			if (bone.hasScale()) copy.setScaleEmpty(false)
			transitionSource!!.addBone(copy)
		}
	}

	/** 跨动画过渡混合：按 blendFactor 在 transitionSource 与 proxyModel 之间 lerp */
	private fun crossfadeStep() {
		val src = transitionSource ?: return
		val boneFlags = activeBoneConfig.resolveBoneFlags(currentAnimTime)
		// 旧动画独有骨骼：每帧重置为 identity
		for ((name, bone) in proxyModel.bones) {
			if (name !in affectedBones) {
				bone.pos.set(0f); bone.rotation.set(0f); bone.scale.set(1f)
			}
		}
		// 补全缺失骨骼（确保新旧集合一致）
		for ((name, _) in proxyModel.bones) {
			if (src.getBone(name) == null) src.addBone(ProxyBone(name))
		}
		for ((name, _) in src.bones) {
			if (proxyModel.getBone(name) == null) proxyModel.addBone(ProxyBone(name))
		}
		for ((name, fromBone) in src.bones) {
			val toBone = proxyModel.getBone(name) ?: continue
			// 不参与混合的骨骼直接使用目标值
			if (!boneFlags[name].shouldBlend()) {
				fromBone.pos.set(toBone.pos); fromBone.rotation.set(toBone.rotation); fromBone.scale.set(toBone.scale)
				continue
			}
			fromBone.pos.lerp(toBone.pos, blendFactor, toBone.pos)
			fromBone.rotation.lerp(toBone.rotation, blendFactor, toBone.rotation)
			fromBone.scale.lerp(toBone.scale, blendFactor, toBone.scale)
			// 根据源/目标数据状态同步 emptyMask
			if (fromBone.hasPos() || toBone.hasPos()) toBone.setPosEmpty(false)
			if (fromBone.hasRot() || toBone.hasRot()) toBone.setRotEmpty(false)
			if (fromBone.hasScale() || toBone.hasScale()) toBone.setScaleEmpty(false)
		}
	}

	// ===== 播放边界检查 =====

	/** 检查动画是否播放完毕，根据 AnimType/LoopType 决定下一状态 */
	private fun checkPlaybackBounds() {
		if (state != State.PLAYING) return
		val info = currentAnimId?.let { getPlaybackInfo() } ?: return
		val animLength = info.animLength
		val endSec = calcEndSecond()
		if (info.animTime < endSec || endSec <= 0f) return

		when (currentConfig.animType) {
			AnimType.PLAY_ONCE, AnimType.DEFAULT -> when {
				isOnceType(info.loopType) -> startFadeOut()
				isLoopType(info.loopType) -> resetAnimAndRestart()
				else -> pause()
			}

			AnimType.LOOP -> {
				resetAnimAndRestart()
				if (currentConfig.startTime > 0) setAnimStartTime(currentConfig.startTime / 20f)
			}

			AnimType.STOP_AT_LAST -> pause()
		}
	}

	/** 判断 LoopType 是否为播放一次 */
	private fun isOnceType(loopType: LoopType): Boolean = loopType == LoopType.ONCE

	/** 判断 LoopType 是否为循环 */
	private fun isLoopType(loopType: LoopType): Boolean = loopType == LoopType.LOOP

	/** 进入淡出状态 */
	private fun startFadeOut() {
		pause()
		state = State.FADING_OUT
		blendTarget = 0f
		currentTransitionTicks = currentConfig.resolveFadeOutTicks(activeBoneConfig.getFadeOutTicks())
	}

	/** 计算动画结束秒数 */
	private fun calcEndSecond(): Float {
		val config = currentConfig
		val info = currentAnimId?.let { getPlaybackInfo() } ?: return 0f
		return when {
			config.endTime < 0 -> info.animLength + config.endTime / 20f
			config.endTime > 0 -> config.endTime / 20f
			else -> info.animLength
		}
	}

	/** 重置动画时间并从开头重新播放 */
	fun resetAnimAndRestart() {
		animTime = 0f
		firedEvents.clear()
	}

	// ===== 游戏刻推进 =====

	final override fun tickAdvance() {
		// 事件钩子：Pre
		if (NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickPre(id, this, manager.mapper)).isCanceled) return
		tickHandlerCall()
		if (state != State.IDLE && state != State.PAUSED) {
			checkPlaybackBounds()
			tickBlend()
			if (state == State.PLAYING) {
				advanceTickCount++
				tickBackend(advanceTickCount / 20f, manager.mapper.holder, manager.mapper.molangData)
			} else {
				// TRANSITIONING / FADING_OUT：冻结时间，用当前 animTime 重算骨骼
				tickBackend(0f, manager.mapper.holder, manager.mapper.molangData, freezeTime = true)
			}
			handleStateTransition()
		}
		NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickPost(id, this, manager.mapper))
	}

	/** 调用 tickHandler 并触发事件钩子 */
	private fun tickHandlerCall() {
		if (NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickHandlerPre(id, this, manager.mapper)).isCanceled) return
		tickHandler(manager.mapper)
		NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickHandlerPost(id, this, manager.mapper))
	}

	/** 处理状态转移：TRANSITIONING→PLAYING / FADING_OUT→IDLE */
	private fun handleStateTransition() {
		if (state == State.TRANSITIONING && transitionSource != null) {
			crossfadeStep()
		}
		if (state == State.TRANSITIONING && blendFactor >= 1f) {
			state = State.PLAYING
			transitionSource = null
			lastRawGameTime = advanceTickCount / 20f
		}
		if (state == State.FADING_OUT && blendFactor <= 0f) {
			forceClear()
		}
	}

	override fun tickRender(deltaSec: Float) {
		// 骨骼清理仅在 PLAYING 时执行（TRANSITIONING 中旧骨骼由 crossfadeStep 维护）
		if (state != State.PLAYING) return
		if (affectedBones.isNotEmpty()) {
			proxyModel.bones.keys.filter { it !in affectedBones }.forEach { proxyModel.bones.remove(it) }
		}
	}

	// ===== 动画后端 =====

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
			manager.queueEvents(this, collectEventsAt(anim))
			return
		}
		val scaledDelta = calcScaledDelta(gameTime)
		val expr = anim.animTimeUpdate
		if (expr != null) {
			val molangData = currentData
			molangData.updateAnimQueries(animTime, scaledDelta)
			animTime = expr.eval(molangData).toFloat()
		} else {
			animTime += scaledDelta
		}
		affectedBones = anim.computeAndWrite(animTime, proxyModel, data)
		manager.queueEvents(this, collectEventsAt(anim))
	}

	/** 计算经过 delta 时间，处理首帧 */
	private fun calcScaledDelta(gameTime: Float): Float {
		if (lastRawGameTime < 0f) {
			lastRawGameTime = gameTime
			animTime = 0f
			return 0f
		}
		val delta = gameTime - lastRawGameTime
		lastRawGameTime = gameTime
		return delta * speedMultiplier
	}

	// ===== 事件收集 =====

	/** 收集当前动画时间点上待触发的音效/粒子/时间线事件 */
	fun collectEventsAt(anim: BakingBrAnimation): AnimationEventsToFire {
		val soundsToFire = mutableListOf<BakingBrAnimationSound>()
		val particlesToFire = mutableListOf<BakingBrAnimationParticle>()
		val timelinesToFire = mutableListOf<BakingBrAnimationTimeline>()

		collectTypedEvents(anim.sounds, "sound_", soundsToFire)
		collectTypedEvents(anim.particles, "particle_", particlesToFire)
		collectTypedEvents(anim.timelines, "timeline_", timelinesToFire)

		return AnimationEventsToFire(soundsToFire, particlesToFire, timelinesToFire)
	}

	/** 收集指定类型的事件（去重） */
	private inline fun <reified T : Any> collectTypedEvents(
		events: List<T>, prefix: String, out: MutableList<T>
	) {
		events.forEachIndexed { i, event ->
			val key = "$prefix$i"
			val time = when (event) {
				is BakingBrAnimationSound -> event.time
				is BakingBrAnimationParticle -> event.time
				is BakingBrAnimationTimeline -> event.time
				else -> return@forEachIndexed
			}
			if (key !in firedEvents && animTime >= time) {
				firedEvents.add(key)
				out.add(event)
			}
		}
	}

	// ===== 内部 =====

	/** 基于 tick 推进 blendFactor */
	private fun tickBlend() {
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

	/** 强制清除并回到 IDLE 状态 */
	private fun forceClear() {
		manager.clearEmittersFor(id)
		state = State.IDLE
		blendFactor = 0f; blendTarget = 0f
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

	/** 转换 Bedrock 循环类型到控制器枚举 */
	fun BakingBrAnimation.LoopType.toLoopType() = when (this) {
		BakingBrAnimation.LoopType.ONCE -> LoopType.ONCE
		BakingBrAnimation.LoopType.LOOP -> LoopType.LOOP
		BakingBrAnimation.LoopType.HOLD_ON_LAST -> LoopType.HOLD_ON_LAST
	}
}
