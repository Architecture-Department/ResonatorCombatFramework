// 动画控制器基类。包含状态机(IDLE/TRANSITIONING/PLAYING/PAUSED/FADING_OUT)、crossfade 过渡系统、blend 混合、播放边界检查。子类实现具体后端
package architecture.resonator_combat_framework.module.player_animation.controller

import architecture.resonator_combat_framework.module.player_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.AnimType
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.player_animation.config.shouldBlend
import architecture.resonator_combat_framework.module.player_animation.registry.ProxyBoneConfigRegistry

abstract class BaseAnimationController @JvmOverloads constructor(
	protected val isClient: Boolean,
	override val isOverriding: Boolean = true
) : IAnimationController {

	protected val configLoader: ProxyBoneConfigRegistry = ProxyBoneConfigRegistry.getInstance(isClient)

	/** 控制器状态机 */
	protected enum class State { IDLE, TRANSITIONING, PLAYING, PAUSED, FADING_OUT }

	val proxyModel = ProxyModel("base")
	protected var state = State.IDLE

	/** 过渡开始时的骨骼快照，用于 crossfade */
	protected var transitionSource: ProxyModel? = null

	/** 当前完整播放配置 */
	protected var currentConfig: AnimationPlayConfig = AnimationPlayConfig.of("")

	/** trigger 时临时覆盖，设完后立即清空 */
	internal var resolvedBoneConfig: ProxyBoneConfigData? = null

	/** 当前活跃骨骼配置（crossfade 时保留引用），trigger 时设，forceClear 时清除 */
	private var activeBoneConfig: ProxyBoneConfigData = ProxyBoneConfigData.EMPTY

	override var blendFactor = 0f
	override var blendTarget = 0f
	override var currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS
	override var speedMultiplier = 1f
	override var currentAnimId: String? = null
	override var affectedBones = emptySet<String>()

	override val currentAnimTime: Float
		get() {
			val info = currentAnimId?.let { getPlaybackInfo(it) } ?: return 0f
			return info.animTime
		}

	override val effectiveWeight: Float get() = if (transitionSource != null) 1f else blendFactor

	override fun isActive(): Boolean = state != State.IDLE

	/** 加载动画数据；返回 false 表示加载失败 */
	protected abstract fun loadAnimation(animId: String): Boolean

	/** 同步后端（如多控制器列表），默认空实现 */
	protected abstract fun syncToBackend(animIds: List<String>, multipliers: List<Float>)

	/** 将所有动画时间归零并清除上一帧记录 */
	protected abstract fun freezeAllAtFrameZero()

	/** 设置动画起始时间（秒） */
	protected abstract fun setAnimStartTime(animId: String, timeSec: Float)

	/** 获取当前动画的播放信息（时间/长度/循环类型） */
	protected abstract fun getPlaybackInfo(animId: String): PlaybackInfo?

	/** 驱动一帧动画计算（子类实现具体后端插值） */
	protected abstract fun tickBackend(gameTime: Float)

	/** 播放信息：当前时间 / 总长度 / 循环类型 */
	data class PlaybackInfo(
		val animTime: Float,
		val animLength: Float,
		val loopType: LoopType
	)

	/** 循环类型 */
	enum class LoopType { ONCE, LOOP, HOLD_ON_LAST }

	// ═══════════════════ 触发 ═══════════════════

	override fun trigger(config: AnimationPlayConfig) {
		speedMultiplier = config.resolveSpeedMultiplier()
		if (!loadAnimation(config.animId)) return
		val finalConfig = resolvedBoneConfig ?: config.boneConfig ?: configLoader.getConfig(config.animId)
		resolvedBoneConfig = null

		// 保存活跃配置供 crossfade 使用
		activeBoneConfig = finalConfig

		snapshotTransitionSource()
		proxyModel.bones.clear()

		currentConfig = config
		currentAnimId = config.animId

		if (config.startTime > 0)
			setAnimStartTime(config.animId, config.startTime / 20f)

		currentTransitionTicks = config.resolveFadeInTicks(finalConfig.transitionTicks)
		state = State.TRANSITIONING
		blendFactor = 0f; blendTarget = 1f
		freezeAllAtFrameZero()
		rebuildBackend()
	}

	override fun trigger(animId: String, transitionTicks: Int) {
		trigger(AnimationPlayConfig(animId = animId, fadeInTicks = transitionTicks, speedMultiplier = speedMultiplier))
	}

	override fun trigger(animId: String, transitionTicks: Int, spd: Float) {
		trigger(AnimationPlayConfig(animId = animId, fadeInTicks = transitionTicks, speedMultiplier = spd))
	}

	override fun triggerForDuration(
		animId: String,
		transitionTicks: Int,
		durationTicks: Int,
		originalAnimLengthSec: Float
	) {
		trigger(
			AnimationPlayConfig(
				animId = animId,
				fadeInTicks = transitionTicks,
				durationTicks = durationTicks,
				originalAnimLengthSec = originalAnimLengthSec,
				speedMultiplier = speedMultiplier
			)
		)
	}

	// ═══════════════════ 停止 ═══════════════════

	override fun stop() = stop(-1)

	override fun stop(fadeOutTicks: Int) {
		if (state == State.IDLE || state == State.FADING_OUT) return
		state = State.FADING_OUT
		blendTarget = 0f
		currentTransitionTicks = if (fadeOutTicks >= 0) fadeOutTicks
		else currentConfig.resolveFadeOutTicks(activeBoneConfig.transitionTicks)
		if (currentTransitionTicks <= 0) forceClear()
	}

	override fun stopImmediate() = forceClear()

	// ═══════════════════ 暂停 / 恢复 ═══════════════════

	override fun pause() {
		if (state == State.PLAYING || state == State.TRANSITIONING) {
			state = State.PAUSED
			transitionSource = null
		}
	}

	override fun resume() {
		if (state != State.PAUSED) return
		val info = currentAnimId?.let { getPlaybackInfo(it) }
		if (info != null && info.animTime * speedMultiplier >= info.animLength
			&& info.loopType == LoopType.HOLD_ON_LAST
		) return
		state = if (isInFadeIn()) State.TRANSITIONING else State.PLAYING
	}

	override fun stopAnimation(animId: String) {
		if (currentAnimId == animId) forceClear()
	}

	override fun restartAnimation(animId: String) {
		restartInternal(animId, configLoader.getConfig(animId))
	}

	// ═══════════════════ 每帧 ═══════════════════

	override fun tick(partialTick: Float, deltaSec: Float) {
		if (state == State.IDLE) return
		tickBlend(deltaSec)

		val shouldTick = state != State.IDLE && state != State.PAUSED

		if (affectedBones.isNotEmpty()) {
			val toRemove = proxyModel.bones.keys.filter { it !in affectedBones }.toList()
			for (name in toRemove) proxyModel.bones.remove(name)
		}

		if (!shouldTick) return

		// 过渡期间不再每帧 freezeAllAtFrameZero，允许动画从第 1 帧开始推进

		checkPlaybackBounds()

		tickBackend(partialTick)

		if (state == State.TRANSITIONING && transitionSource != null) {
			crossfadeStep()
		}

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

	/**
	 * 跨动画过渡混合。
	 * 检查 each bone 的 shouldBlend 标志：false 时不 lerp，直接使用新动画数据。
	 */
	private fun crossfadeStep() {
		val src = transitionSource ?: return
		val boneFlags = activeBoneConfig.resolveBoneFlags(currentAnimTime)

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
		// lerp — 跳过 shouldBlend=false 的骨骼
		for ((name, fromBone) in src.bones) {
			val toBone = proxyModel.getBone(name) ?: continue
			if (!boneFlags[name].shouldBlend()) {
				// 不混合：直接复制新动画数据到源，确保渲染时无旧动画残留
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

	// ═══════════════════ 播放边界检查 ═══════════════════

	private fun checkPlaybackBounds() {
		if (state != State.PLAYING) return
		val info = currentAnimId?.let { getPlaybackInfo(it) } ?: return
		val effectiveTime = info.animTime  // animTime 已包含速度缩放
		val config = currentConfig
		val animLength = info.animLength

		val endSec = when {
			config.endTime < 0 -> animLength + config.endTime / 20f
			config.endTime > 0 -> config.endTime / 20f
			else -> animLength
		}
		if (effectiveTime < endSec || endSec <= 0f) return

		when (config.animType) {
			AnimType.PLAY_ONCE -> {
				pause()
				state = State.FADING_OUT
				blendTarget = 0f
				currentTransitionTicks = currentConfig.resolveFadeOutTicks(activeBoneConfig.transitionTicks)
			}

			AnimType.LOOP -> {
				resetAnimAndRestart(config)
				if (config.startTime > 0) setAnimStartTime(config.animId, config.startTime / 20f)
			}

			AnimType.STOP_AT_LAST -> pause()
			AnimType.DEFAULT -> {
				when (info.loopType) {
					LoopType.ONCE -> {
						pause()
						state = State.FADING_OUT
						blendTarget = 0f
						currentTransitionTicks = currentConfig.resolveFadeOutTicks(activeBoneConfig.transitionTicks)
					}

					LoopType.LOOP -> resetAnimAndRestart(config)
					LoopType.HOLD_ON_LAST -> pause()
				}
			}
		}
	}

	/** 重置动画时间并从开头重新播放 */
	protected abstract fun resetAnimAndRestart(config: AnimationPlayConfig)

	// ═══════════════════ 内部 ═══════════════════

	/** 内部：无过渡地立即重新播放（仅 restartAnimation 使用） */
	private fun restartInternal(animId: String, config: ProxyBoneConfigData) {
		currentAnimId = animId
		activeBoneConfig = config
		state = State.PLAYING; blendTarget = 1f; blendFactor = 1f
		transitionSource = null
		resetAnimAndRestart(currentConfig)
	}

	/** 将当前动画 ID 和速度同步到后端 */
	private fun rebuildBackend() {
		val ids = currentAnimId?.let { listOf(it) } ?: emptyList()
		val multipliers = ids.map { speedMultiplier }
		syncToBackend(ids, multipliers)
	}

	/** 在 ds 秒内按 currentTransitionTicks 推进 blendFactor 到 blendTarget */
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

	/** 强制清除所有状态，回到 IDLE */
	private fun forceClear() {
		state = State.IDLE
		blendFactor = 0f; blendTarget = 0f
		proxyModel.bones.clear()
		transitionSource = null
		currentConfig = AnimationPlayConfig.of("")
		currentAnimId = null; affectedBones = emptySet()
		currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS
		speedMultiplier = 1f
		activeBoneConfig = ProxyBoneConfigData.EMPTY
		rebuildBackend()
	}

	/** 当前是否处于淡入阶段（blendTarget > 0 且 blendFactor 未达目标） */
	private fun isInFadeIn(): Boolean = blendTarget > 0f && blendFactor < 1f
}
