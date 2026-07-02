package architecture.resonator_combat_framework.module.entity_animation.animation.controller

import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationDef
import architecture.resonator_combat_framework.module.entity_animation.animation.LoopType
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.KeyframeAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController.State
import architecture.resonator_combat_framework.module.entity_animation.animation.data.BoneConfig
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayConfig
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayMode
import architecture.resonator_combat_framework.module.entity_animation.animation.data.shouldBlend
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BonePose
import architecture.resonator_combat_framework.module.entity_animation.animation.model.GeometryData
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.module.entity_animation.registry.AnimationDefRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.BoneConfigRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.GeometryModelRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.KeyframeAnimationRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import architecture.resonator_combat_framework.util.RotationUtil
import architecture.resonator_combat_framework.util.TimeUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

/**
 * 动画控制器——[IEntityAnimationController] 的默认实现。
 *
 * 管理动画的触发、播放、过渡（crossfade）、暂停/恢复、循环及事件触发等完整生命周期。
 *
 * @param manager 所属的动画控制器管理器
 * @param id 控制器唯一标识
 * @param isClient 是否为客户端
 * @param isOverriding 是否覆盖模式（true=覆盖低优先级动画，false=叠加）
 */
class AnimationController<T : Entity> @JvmOverloads constructor(
	override val manager: AnimationControllerManager<T>,
	override val id: ResourceLocation,
	protected val isClient: Boolean,
	override val isOverriding: Boolean = true
) : IEntityAnimationController<T> {

	val poseData = PoseData("base")

	final override var state = State.IDLE; private set
	final override var transitionSource: PoseData? = null; private set
	final override var currentConfig: PlayConfig = PlayConfig.EMPTY; private set

	/** 当前 tick 的动画数据（来自 [KeyframeAnimationRegistry]，随 [currentAnim] 切换） */
	final override var currentBakingAnim: KeyframeAnimation? = null; private set

	override var localBoneConfig: BoneConfig = BoneConfig.EMPTY
		set(value) {
			if (currentConfig.mirror) {
				value.mirror()
			}
			field = value
			_activeBoneConfig = null
		}

	/** 当前的骨骼配置 */
	final override var currentBoneConfig: BoneConfig = BoneConfig.EMPTY
		private set(value) {
			field = value
			_activeBoneConfig = value
		}

	/** 当前生效的骨骼配置缓存（合并 localBoneConfig 后的结果） */
	private var _activeBoneConfig: BoneConfig? = null

	final override val activeBoneConfig: BoneConfig
		get() {
			if (_activeBoneConfig == null) {
				_activeBoneConfig = currentBoneConfig.merge(localBoneConfig)
			}
			return _activeBoneConfig!!
		}

	final override var fadeProgress = 0f; private set
	final override var fadeTarget = 0f; private set
	final override var currentTransitionTicks = BoneConfig.DEFAULT_TRANSITION_TICKS; private set
	final override var speedMultiplier = 1f
	final override var affectedBones = emptySet<String>(); private set

	protected final var advanceTickCount = 0L; private set
	final override var currentAnim: AnimationDef? = null; private set
	private final val firedEvents = mutableSetOf<String>()
	final override var extraModel: GeometryData? = null; private set
	protected final var animTime = 0f; private set
	protected final var lastRawGameTime = -1f; private set

	override val currentAnimTime: Float get() = currentAnim?.let { animTime } ?: 0f
	override val mergeWeight: Float get() = if (transitionSource != null) 1f else fadeProgress
	override val isFadingOut: Boolean get() = state == State.FADING_OUT
	override val isFadingIn: Boolean get() = state == State.CROSSFADING

	// ===== 动画数据查询 =====

	private fun currentLength(): Float = currentBakingAnim?.length ?: 0f
	private fun currentLoopType(): LoopType = currentBakingAnim?.loop ?: LoopType.ONCE

	override fun isActive(): Boolean = state != State.IDLE

	// ===== 触发 =====

	override fun trigger(animId: ResourceLocation, config: PlayConfig) {
		val anim = AnimationDefRegistry.get(animId)
		if (anim == null || anim.get() == null) {
			RcfUtil.LOGGER.warn("[AnimDebug] Animation not found: $animId")
			return
		}
		trigger(anim.get()!!, config)
	}

	override fun trigger(anim: AnimationDef, config: PlayConfig) {
		RcfEventHooks.AnimationTriggerPre(this, anim, config)

		val oldActionAnim = currentAnim
		oldActionAnim?.onEnd(manager.holder)

		currentAnim = anim
		val baseAnim = KeyframeAnimationRegistry.find(anim.animationId) ?: KeyframeAnimation.EMPTY
		currentBakingAnim = if (config.mirror) baseAnim.mirror() else baseAnim
		val baseConfig = BoneConfigRegistry.find(anim.animationId) ?: BoneConfig.EMPTY
		currentBoneConfig = if (config.mirror) baseConfig.mirror() else baseConfig
		manager.clearEmittersFor(id)
		poseData.bones.clear()
		firedEvents.clear()
		speedMultiplier = config.resolveSpeedMultiplier()
		currentConfig = config

		snapshotTransitionSource()
		currentTransitionTicks = config.resolveFadeInTicks(activeBoneConfig.getFadeInTicks())
		state = State.CROSSFADING
		fadeProgress = 0f
		fadeTarget = 1f

		// 根据播放方向设置初始动画时间
		animTime = if (speedMultiplier >= 0) 0f else calcEndSecond()
		if (config.startTime > 0) animTime = config.startTime / 20f
		lastRawGameTime = -1f
		affectedBones = anim.computeAndWrite(currentBakingAnim!!, animTime, poseData, currentData)
		if (transitionSource != null) crossfadeStep()

		anim.onBegin(manager.holder, animTime, 0f, poseData, manager.brModel)
		anim.tick(manager.holder, animTime, 0f, poseData, manager.brModel)
		extraModel = activeBoneConfig.extraModelId?.let { GeometryModelRegistry.find(it) }
		manager.rebuildBones()

		anim.onStart(manager.holder, animTime, 0f, poseData, manager.brModel)
		RcfEventHooks.AnimationTriggerPost(this, anim, config)
	}

	// ===== 停止 =====

	override fun stop(fadeOutTicks: Int) {
		if (state == State.IDLE || state == State.FADING_OUT) return
		manager.clearEmittersFor(id)
		state = State.FADING_OUT
		fadeTarget = 0f
		transitionSource = null
		currentTransitionTicks = if (fadeOutTicks >= 0) fadeOutTicks
		else currentConfig.resolveFadeOutTicks(activeBoneConfig.getFadeOutTicks())
		if (currentTransitionTicks <= 0) forceClear()
	}

	override fun pause() {
		if (state == State.PLAYING || state == State.CROSSFADING) {
			state = State.PAUSED; transitionSource = null
		}
	}

	override fun resume() {
		if (state != State.PAUSED) return
		if (currentAnim == null) {
			state = if (isInFadeIn()) State.CROSSFADING else State.PLAYING
			return
		}

		if (speedMultiplier >= 0) {
			if (animTime * speedMultiplier >= currentLength() && currentLoopType() == LoopType.HOLD_ON_LAST) return
		} else {
			if (animTime <= 0f && currentLoopType() == LoopType.HOLD_ON_LAST) return
		}
		state = if (isInFadeIn()) State.CROSSFADING else State.PLAYING
	}

	/**
	 * tick 处理钩子——由子类重写以实现自定义 tick 行为。
	 */
	fun tickHandler(manager: IEntityAnimationMapperProvider<T, *>) {}

	// ===== crossfade =====

	/**
	 * 记录当前姿态快照作为 crossfade 过渡的源。
	 * 新动画触发时保存旧动画的骨骼数据，用于后续混合。
	 */
	private fun snapshotTransitionSource() {
		if (state == State.IDLE || poseData.bones.isEmpty()) return
		transitionSource = PoseData("src")
		for ((name, bone) in poseData.bones) {
			val copy = BonePose(name)
			copy.pos.set(bone.pos); copy.rotation.set(bone.rotation); copy.scale.set(bone.scale)
			if (bone.hasPos()) copy.setPosEmpty(false)
			if (bone.hasRot()) copy.setRotEmpty(false)
			if (bone.hasScale()) copy.setScaleEmpty(false)
			transitionSource!!.addBone(copy)
		}
	}

	/**
	 * 执行一帧 crossfade 混合——从过渡源姿态逐步过渡到当前动画姿态。
	 * 根据 [fadeProgress] 对每根骨骼的位置/旋转/缩放进行线性插值。
	 */
	private fun crossfadeStep() {
		val src = transitionSource ?: return
		val boneFlags = activeBoneConfig.resolveBoneFlags(currentAnimTime)
		for ((name, bone) in poseData.bones) {
			if (name !in affectedBones) bone.pos.set(0f); bone.rotation.set(0f); bone.scale.set(1f)
		}

		for ((name, _) in poseData.bones) {
			if (src.getBone(name) == null) src.addBone(BonePose(name))
		}

		for ((name, _) in src.bones) {
			if (poseData.getBone(name) == null) poseData.addBone(BonePose(name))
		}

		for ((name, fromBone) in src.bones) {
			val toBone = poseData.getBone(name) ?: continue
			if (!boneFlags[name].shouldBlend()) {
				fromBone.pos.set(toBone.pos); fromBone.rotation.set(toBone.rotation); fromBone.scale.set(toBone.scale)
				continue
			}

			fromBone.pos.lerp(toBone.pos, fadeProgress, toBone.pos)
			RotationUtil.lerpRotation(fromBone.rotation, toBone.rotation, fadeProgress, toBone.rotation)
			fromBone.scale.lerp(toBone.scale, fadeProgress, toBone.scale)
			if (fromBone.hasPos() || toBone.hasPos()) toBone.setPosEmpty(false)
			if (fromBone.hasRot() || toBone.hasRot()) toBone.setRotEmpty(false)
			if (fromBone.hasScale() || toBone.hasScale()) toBone.setScaleEmpty(false)
		}
	}

	// ===== 播放边界 =====

	/**
	 * 检查播放边界——动画播放到末尾后的处理。
	 * 根据循环类型和播放模式决定停止、循环或保持最后一帧。
	 */
	private fun checkPlaybackBounds() {
		if (state != State.PLAYING) return
		val anim = currentAnim ?: return

		if (speedMultiplier < 0) {
			if (animTime > 0f) return
		} else {
			val endSec = calcEndSecond()
			if (animTime < endSec || endSec <= 0f) return
		}

		when (currentConfig.playMode) {
			PlayMode.PLAY_ONCE, PlayMode.DEFAULT -> when {
				currentLoopType() == LoopType.ONCE -> startFadeOut()
				currentLoopType() == LoopType.LOOP -> resetAnimAndRestart()
				else -> pause()
			}

			PlayMode.LOOP -> {
				resetAnimAndRestart(); if (currentConfig.startTime > 0) setAnimStartTime(currentConfig.startTime / 20f)
			}

			PlayMode.STOP_AT_LAST -> pause()
		}
	}

	/** 开始淡出流程，触发动画完成事件 */
	private fun startFadeOut() {
		pause()
		state = State.FADING_OUT
		fadeTarget = 0f
		currentTransitionTicks = currentConfig.resolveFadeOutTicks(activeBoneConfig.getFadeOutTicks())
		RcfEventHooks.AnimationComplete(this)
	}

	/** 计算动画结束时间（秒），支持正放和倒放 */
	private fun calcEndSecond(): Float {
		val config = currentConfig
		val animLength = currentLength()
		if (animLength <= 0f) return 0f
		return when {
			config.endTime < 0 -> animLength + config.endTime / 20f
			config.endTime > 0 -> config.endTime / 20f
			else -> animLength
		}
	}

	/** 重置动画时间到起点（根据播放方向），清除已触发事件记录 */
	fun resetAnimAndRestart() {
		animTime = if (speedMultiplier >= 0) 0f else calcEndSecond()
		firedEvents.clear()
	}

	// ===== 游戏刻推进 =====

	final override fun tick() {
		if (RcfEventHooks.AnimationControllerTickPre(id, this, manager.mapperProvider)) return
		tickHandlerCall()
		if (state == State.IDLE || state == State.PAUSED) {
			RcfEventHooks.AnimationControllerTickPost(id, this, manager.mapperProvider)
			return
		}
		checkPlaybackBounds()
		tickBlend()
		if (state == State.PLAYING) {
			advanceTickCount++; tickBackend(advanceTickCount / 20f)
		} else tickBackend(0f, freezeTime = true)
		handleStateTransition()
		RcfEventHooks.AnimationControllerTickPost(id, this, manager.mapperProvider)
	}

	override fun tickAdvance() {
		val anim = currentAnim ?: return
		anim.tickAdvance(manager.holder, currentAnimTime, poseData, manager.brModel, manager.mergedPose, this)
	}

	private fun tickHandlerCall() {
		if (RcfEventHooks.AnimationControllerTickHandlerPre(id, this, manager.mapperProvider)) return
		tickHandler(manager.mapperProvider)
		RcfEventHooks.AnimationControllerTickHandlerPost(id, this, manager.mapperProvider)
	}

	/**
	 * 处理状态转换：
	 * - CROSSFADING 中持续执行 crossfade 混合
	 * - CROSSFADING 完成后切换到 PLAYING
	 * - FADING_OUT 完成后清理动画并回到 IDLE
	 */
	private fun handleStateTransition() {
		if (state == State.CROSSFADING && transitionSource != null) crossfadeStep()
		if (state == State.CROSSFADING && fadeProgress >= 1f) {
			state = State.PLAYING; transitionSource = null; lastRawGameTime = advanceTickCount / 20f
		}
		if (state == State.FADING_OUT && fadeProgress <= 0f) forceClear()
	}

	override fun tickRender(deltaSec: Float) {
		if (state != State.PLAYING) return
		if (affectedBones.isNotEmpty()) poseData.bones.keys.filter { it !in affectedBones }
			.forEach { poseData.bones.remove(it) }
	}

	// ===== 动画后端 =====

	/** 将所有骨骼冻结在零帧位置 */
	fun freezeAllAtFrameZero() {
		animTime = 0f; lastRawGameTime = -1f
	}

	/**
	 * 手动设置动画起始时间（秒）。
	 * 用于从指定时间点继续播放。
	 */
	fun setAnimStartTime(timeSec: Float) {
		animTime = timeSec
	}

	/**
	 * 后端 tick——执行动画姿态计算和事件收集。
	 *
	 * @param gameTime 当前游戏时间（秒）
	 * @param freezeTime 是否冻结时间（true 时仅计算当前帧姿态，不推进时间）
	 */
	fun tickBackend(gameTime: Float, freezeTime: Boolean = false) {
		val anim = currentAnim ?: return
		val data = currentData
		val bakingAnim = currentBakingAnim ?: return
		if (freezeTime) {
			affectedBones = anim.computeAndWrite(bakingAnim, animTime, poseData, data)
			manager.queueEvents(this, anim.collectEvents(bakingAnim, animTime, animTime, firedEvents))
			return
		}
		val scaledDelta = calcScaledDelta(gameTime)
		val prevAnimTime = animTime
		animTime = anim.tickAnimTime(animTime, scaledDelta)
		data.updateAnimQueries(animTime, scaledDelta)
		affectedBones = anim.computeAndWrite(bakingAnim, animTime, poseData, data)
		anim.tick(manager.holder, animTime, scaledDelta, poseData, manager.brModel)
		manager.queueEvents(this, anim.collectEvents(bakingAnim, animTime, prevAnimTime, firedEvents))
	}

	/**
	 * 计算缩放后的 delta 时间。
	 * 结合 speedMultiplier 和实际游戏时间差计算动画步进。
	 */
	private fun calcScaledDelta(gameTime: Float): Float {
		if (lastRawGameTime < 0f) {
			lastRawGameTime = gameTime; return 0f
		}
		val delta = gameTime - lastRawGameTime; lastRawGameTime = gameTime; return TimeUtil.calcScaledDelta(
			delta,
			speedMultiplier
		)
	}

	// ===== 内部 =====

	/** 按帧推进 fadeProgress 向 fadeTarget 靠近 */
	private fun tickBlend() {
		if (fadeProgress == fadeTarget) return
		if (currentTransitionTicks <= 0) {
			fadeProgress = fadeTarget; return
		}
		val step = 1f / currentTransitionTicks
		fadeProgress = if (fadeProgress < fadeTarget) (fadeProgress + step).coerceAtMost(fadeTarget)
		else (fadeProgress - step).coerceAtLeast(fadeTarget)
	}

	/**
	 * 强制清理所有动画状态并回到 IDLE。
	 * 在淡出完成或动画显式停止时调用。
	 */
	private fun forceClear() {
		currentAnim?.onEnd(manager.holder)
		manager.clearEmittersFor(id)
		state = State.IDLE
		fadeProgress = 0f
		fadeTarget = 0f
		poseData.bones.clear()
		transitionSource = null
		currentConfig = PlayConfig.EMPTY
		affectedBones = emptySet()
		currentTransitionTicks = BoneConfig.DEFAULT_TRANSITION_TICKS
		speedMultiplier = 1f
		extraModel = null
		currentBakingAnim = null
		currentBoneConfig = BoneConfig.EMPTY
		_activeBoneConfig = null
		manager.rebuildBones()
	}

	private fun isInFadeIn(): Boolean = isFadingIn
	override fun equalsCurrentAnimId(id: ResourceLocation): Boolean = currentAnim?.animationId == id
}
