package architecture.resonator_combat_framework.module.player_animation.controller

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig

/** 动画控制器：过渡状态 + 生命周期 */
@AllOpe
interface IAnimationController {
	var blendFactor: Float
	var blendTarget: Float
	var currentTransitionTicks: Int
	var speedMultiplier: Float
	var priority: Int
	var isOverriding: Boolean
	var currentAnimId: String?
	var affectedBones: Set<String>

	fun isActive(): Boolean
	val effectiveWeight: Float
	val currentAnimTime: Float
	fun tick(partialTick: Float, deltaSec: Float)

	/** 详细播放配置 */
	fun trigger(config: AnimationPlayConfig)

	/** 触发动画，传入过渡 tick */
	fun trigger(animId: String, transitionTicks: Int)

	/** 触发动画，指定过渡 tick 和速度 */
	fun trigger(animId: String, transitionTicks: Int, speedMultiplier: Float)

	/** 触发动画，自动计算速度以适配 durationTicks */
	fun triggerForDuration(animId: String, transitionTicks: Int, durationTicks: Int, originalAnimLengthSec: Float)

	fun stop() = stop(-1)

	/** 停止动画，指定淡出时间 */
	fun stop(fadeOutTicks: Int)
	fun stopImmediate()
	fun pause()
	fun resume()
	fun stopAnimation(animId: String)
	fun restartAnimation(animId: String)
}
