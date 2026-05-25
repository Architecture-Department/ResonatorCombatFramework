package architecture.resonator_combat_framework.module.player_animation.controller

import architecture.resonator_combat_framework.module.player_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.config.AnimType
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigLoader

/**
 * 动画控制器基类。
 *
 * 包含状态机、过渡系统、crossfade 等通用逻辑。
 * 子类只需实现后端相关方法（加载动画、驱动帧、写入骨骼等）。
 */
abstract class BaseAnimationController(
	protected val isClient: Boolean,
	protected val configLoader: ProxyBoneConfigLoader = ProxyBoneConfigLoader.getInstance(isClient)
) : IAnimationController {

	protected enum class State { IDLE, TRANSITIONING, PLAYING, PAUSED, FADING_OUT }

	val proxyModel = ProxyModel("base")
	protected var state = State.IDLE
	protected var transitionSource: ProxyModel? = null
	protected var currentConfig: AnimationPlayConfig = AnimationPlayConfig.of("")

	override var blendFactor = 0f
	override var blendTarget = 0f
	override var currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS
	override var speedMultiplier = 1f
	override var priority = 0
	override var isOverriding = true
	override var currentAnimId: String? = null
	override var affectedBones = emptySet<String>()

	protected var lastTickSec = 0f

	/** 跨动画过渡时恒为 1.0；否则等于 blendFactor */
	val effectiveWeight: Float get() = if (transitionSource != null) 1f else blendFactor

	override fun isActive(): Boolean = state != State.IDLE

	// ═══════════════════ 抽象：后端操作 ═══════════════════

	/** 加载动画，返回 true 表示成功 */
	protected abstract fun loadAnimation(animId: String): Boolean

	/** 将当前动画列表同步到后端 */
	protected abstract fun syncToBackend(animIds: List<String>, multipliers: List<Float>)

	/** 冻结全部动画到第 0 帧 */
	protected abstract fun freezeAllAtFrameZero()

	/** 设置动画起始时间（秒）*/
	protected abstract fun setAnimStartTime(animId: String, timeSec: Float)

	/** 获取动画播放信息，null 表示取不到 */
	protected abstract fun getPlaybackInfo(animId: String): PlaybackInfo?

	/** 每帧驱动后端，将骨骼写入 proxyModel */
	protected abstract fun tickBackend(ticks: Float)

	data class PlaybackInfo(
		val animTime: Float,
		val animLength: Float,
		val loopType: LoopType
	)

	enum class LoopType { ONCE, LOOP, HOLD_ON_LAST }

	// ═══════════════════ 触发 ═══════════════════

	override fun trigger(config: AnimationPlayConfig) {
		if (!loadAnimation(config.animId)) return
		val cfg = configLoader.getConfig(config.animId)
		val finalConfig = config.boneConfig ?: cfg

		if (currentAnimId == config.animId && state != State.IDLE) {
			restartInternal(config.animId, finalConfig)
			return
		}

		// 跨动画过渡源
		snapshotTransitionSource()

		currentConfig = config
		speedMultiplier = config.resolveSpeedMultiplier()
		currentAnimId = config.animId
		affectedBones = finalConfig.resolveCurrentBoneNames()

		if (config.startTime > 0 && isClient)
			setAnimStartTime(config.animId, config.startTime / 20f)

		currentTransitionTicks = config.resolveFadeInTicks(cfg.transitionTicks)
		state = State.TRANSITIONING
		blendFactor = 0f; blendTarget = 1f
		lastTickSec = 0f
		freezeAllAtFrameZero()
		rebuildBackend()
	}

	override fun trigger(animId: String, transitionTicks: Int) {
		trigger(AnimationPlayConfig.of(animId).copy(fadeInTicks = transitionTicks, speedMultiplier = speedMultiplier))
	}

	override fun trigger(animId: String, transitionTicks: Int, spd: Float) {
		trigger(AnimationPlayConfig.of(animId).copy(fadeInTicks = transitionTicks, speedMultiplier = spd))
	}

	override fun triggerForDuration(
		animId: String,
		transitionTicks: Int,
		durationTicks: Int,
		originalAnimLengthSec: Float
	) {
		trigger(
			AnimationPlayConfig.of(animId).copy(
				fadeInTicks = transitionTicks,
				durationTicks = durationTicks,
				originalAnimLengthSec = originalAnimLengthSec,
				speedMultiplier = speedMultiplier
			)
		)
	}

	// ═══════════════════ 停止 ═══════════════════

	override fun stop() {
		if (state == State.IDLE || state == State.FADING_OUT) return
		state = State.FADING_OUT
		blendTarget = 0f
		currentTransitionTicks = currentConfig.resolveFadeOutTicks(currentTransitionTicks)
		if (currentTransitionTicks <= 0) forceClear()
	}

	override fun stopImmediate() = forceClear()

	// ═══════════════════ 暂停 / 恢复 ═══════════════════

	override fun pause() {
		if (state == State.PLAYING || state == State.TRANSITIONING) state = State.PAUSED
	}

	override fun resume() {
		if (state == State.PAUSED) state = if (isInFadeIn()) State.TRANSITIONING else State.PLAYING
	}

	override fun stopAnimation(animId: String) {
		if (currentAnimId == animId) forceClear()
	}

	override fun restartAnimation(animId: String) {
		restartInternal(animId, configLoader.getConfig(animId))
	}

	// ═══════════════════ 每帧 ═══════════════════

	override fun tick(partialTick: Float, deltaSec: Float) {
		if (!isClient) return
		val ticks = partialTick  // 由调用方传入的累计秒
		val dSec = if (lastTickSec == 0f) 0f else ticks - lastTickSec
		lastTickSec = ticks

		tickBlend(dSec)

		val shouldTick = state != State.IDLE && state != State.PAUSED

		// 清除不属于当前动画的骨骼
		if (affectedBones.isNotEmpty()) {
			val toRemove = proxyModel.bones.keys.filter { it !in affectedBones }.toList()
			for (name in toRemove) proxyModel.bones.remove(name)
		}

		if (!shouldTick) return

		// 冻结首帧
		if (state == State.TRANSITIONING) freezeAllAtFrameZero()

		checkPlaybackBounds()

		// 子类驱动后端 + 写入 proxyModel
		tickBackend(ticks)

		// crossfade
		if (state == State.TRANSITIONING && transitionSource != null) {
			crossfadeStep()
		}

		// 状态转换
		if (state == State.TRANSITIONING && blendFactor >= 1f) {
			state = State.PLAYING
			transitionSource = null
		}
		if (state == State.FADING_OUT && blendFactor <= 0f) forceClear()
	}

	// ═══════════════════ crossfade ═══════════════════

	private fun snapshotTransitionSource() {
		if (state == State.IDLE || proxyModel.bones.isEmpty()) return
		transitionSource = ProxyModel("src")
		for ((name, bone) in proxyModel.bones) {
			val copy = ProxyBone(name)
			copy.pos.set(bone.pos); copy.rotation.set(bone.rotation); copy.scale.set(bone.scale)
			transitionSource!!.addBone(copy)
		}
	}

	private fun crossfadeStep() {
		val src = transitionSource ?: return

		// 旧动画独有骨骼：每帧重置为 identity
		for ((name, bone) in proxyModel.bones) {
			if (name !in affectedBones)
				bone.pos.set(0f).also { bone.rotation.set(0f); bone.scale.set(1f) }
		}
		// 新动画骨骼：source 补 identity
		for ((name, _) in proxyModel.bones) {
			if (src.getBone(name) == null) src.addBone(ProxyBone(name))
		}
		// 旧动画骨骼：target 补 identity
		for ((name, _) in src.bones) {
			if (proxyModel.getBone(name) == null) proxyModel.addBone(ProxyBone(name))
		}
		// lerp
		for ((name, fromBone) in src.bones) {
			val toBone = proxyModel.getBone(name) ?: continue
			fromBone.pos.lerp(toBone.pos, blendFactor, toBone.pos)
			fromBone.rotation.lerp(toBone.rotation, blendFactor, toBone.rotation)
			fromBone.scale.lerp(toBone.scale, blendFactor, toBone.scale)
		}
	}

	// ═══════════════════ 播放边界检查 ═══════════════════

	private fun checkPlaybackBounds() {
		val info = currentAnimId?.let { getPlaybackInfo(it) } ?: return
		val effectiveTime = info.animTime * speedMultiplier
		val config = currentConfig
		val animLength = info.animLength

		val endSec = when {
			config.endTime < 0 -> animLength + config.endTime / 20f
			config.endTime > 0 -> config.endTime / 20f
			else -> animLength
		}
		if (effectiveTime < endSec) return

		when (config.animType) {
			AnimType.PLAY_ONCE -> stop()
			AnimType.LOOP -> {
				resetAnimAndRestart(config)
				if (config.startTime > 0) setAnimStartTime(config.animId, config.startTime / 20f)
			}

			AnimType.STOP_AT_LAST -> pause()
			AnimType.DEFAULT -> {
				when (info.loopType) {
					LoopType.ONCE -> stop()
					LoopType.LOOP -> resetAnimAndRestart(config)
					LoopType.HOLD_ON_LAST -> pause()
				}
			}
		}
	}

	protected abstract fun resetAnimAndRestart(config: AnimationPlayConfig)

	// ═══════════════════ 内部 ═══════════════════

	private fun restartInternal(animId: String, config: ProxyBoneConfigData) {
		currentAnimId = animId
		affectedBones = config.resolveCurrentBoneNames()
		state = State.PLAYING; blendTarget = 1f; blendFactor = 1f
		transitionSource = null
		if (isClient) resetAnimAndRestart(currentConfig)
		lastTickSec = 0f
	}

	private fun rebuildBackend() {
		if (!isClient) return
		val ids = currentAnimId?.let { listOf(it) } ?: emptyList()
		val multipliers = ids.map { speedMultiplier }
		syncToBackend(ids, multipliers)
	}

	private fun tickBlend(ds: Float) {
		if (blendFactor == blendTarget) return
		if (currentTransitionTicks <= 0) {
			blendFactor = blendTarget; return
		}
		val step = (ds * 20f) / currentTransitionTicks
		blendFactor = if (blendFactor < blendTarget)
			(blendFactor + step).coerceAtMost(blendTarget)
		else
			(blendFactor - step).coerceAtLeast(blendTarget)
	}

	private fun forceClear() {
		state = State.IDLE
		blendFactor = 0f; blendTarget = 0f
		proxyModel.bones.clear()
		transitionSource = null
		currentAnimId = null; affectedBones = emptySet()
		lastTickSec = 0f
		rebuildBackend()
	}

	private fun isInFadeIn(): Boolean = blendTarget > 0f && blendFactor < 1f
}
