package architecture.resonator_combat_framework.module.player_animation.controller

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig

/**
 * 动画控制器接口。
 *
 * 定义动画控制器的生命周期：触发 → 过渡 → 播放 → 停止/淡出。
 * 每个控制器独立管理自己的状态机和骨骼代理数据。
 */
@AllOpe
interface IAnimationController {
	// ═══════════════════ 属性 ═══════════════════

	/** 当前混合因子 0..1，用于淡入淡出和过渡混合 */
	var blendFactor: Float

	/** 混合目标值：1 = 完全播放，0 = 完全隐藏 */
	var blendTarget: Float

	/** 当前过渡 tick 数（淡入或淡出持续时间） */
	var currentTransitionTicks: Int

	/** 播放速度倍数 */
	var speedMultiplier: Float

	/** 是否覆盖低优先级控制器的骨骼 */
	val isOverriding: Boolean

	/** 当前播放的动画 ID */
	var currentAnimId: String?

	/** 当前动画实际控制的骨骼名称集合 */
	var affectedBones: Set<String>

	/** 是否处于活跃状态（非 IDLE） */
	fun isActive(): Boolean

	/**
	 * 跨动画过渡时的有效权重。
	 * 过渡阶段恒为 1.0（由 crossfade 内部处理），
	 * 否则等于 [blendFactor]。
	 */
	val effectiveWeight: Float

	/**
	 * 当前动画时间（秒），用于骨骼配置 timeline 求值。
	 * 已计入速度缩放。
	 */
	val currentAnimTime: Float

	/**
	 * 每帧驱动逻辑：更新混合因子、推进动画时间、计算骨骼数据。
	 * @param partialTick 当前渲染 tick（含小数部分）
	 * @param deltaSec 距上一帧的秒数
	 */
	fun tick(partialTick: Float, deltaSec: Float)

	// ═══════════════════ 触发 ═══════════════════

	/** 使用完整配置触发动画 */
	fun trigger(config: AnimationPlayConfig)

	/** 触发动画，仅指定过渡 tick 数（沿用当前速度） */
	fun trigger(animId: String, transitionTicks: Int)

	/** 触发动画，指定过渡 tick 数和速度 */
	fun trigger(animId: String, transitionTicks: Int, speedMultiplier: Float)

	/** 触发动画，自动计算速度以在指定 tick 数内播完 */
	fun triggerForDuration(animId: String, transitionTicks: Int, durationTicks: Int, originalAnimLengthSec: Float)

	// ═══════════════════ 停止 ═══════════════════

	/** 停止动画（使用配置中的淡出时间） */
	fun stop() = stop(-1)

	/** 停止动画，指定淡出时间（-1 使用配置值） */
	fun stop(fadeOutTicks: Int)

	/** 立即停止动画，无过渡 */
	fun stopImmediate()

	// ═══════════════════ 暂停 / 恢复 ═══════════════════

	/** 暂停动画，保持当前骨骼姿态 */
	fun pause()

	/** 恢复暂停的动画 */
	fun resume()

	// ═══════════════════ 动画管理 ═══════════════════

	/** 停止指定动画 ID（在播放相同 id 时有效） */
	fun stopAnimation(animId: String)

	/** 重新启动当前动画 */
	fun restartAnimation(animId: String)
}