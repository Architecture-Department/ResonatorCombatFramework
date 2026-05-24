package architecture.resonator_combat_framework.module.player_animation.controller

/** 动画控制器 — 管理单个控制器的过渡状态和动画生命周期 */
interface IAnimationController {
	/** 当前混合权重 (0=原版, 1=完全动画) */
	var blendFactor: Float

	/** 混合目标值 */
	var blendTarget: Float

	/** 过渡用时 (tick制, 0=即时) */
	var currentTransitionTicks: Int

	/**
	 * 动画播放速度倍数.
	 * 1.0 = 原速, 2.0 = 双倍速, 0.5 = 半速.
	 * 与 triggerForDuration 互斥: triggerForDuration 会自动覆盖此值.
	 */
	var speedMultiplier: Float

	/** 控制器优先级 (数值越大优先级越高). 高优先级控制器会覆盖低优先级控制器的同骨骼动画 */
	var priority: Int

	/** 是否覆盖模式 (默认开启). 开启时此控制器不会与其他控制器叠加混合 */
	var isOverriding: Boolean

	/** 当前播放的动画 ID, 无动画时为 null. 同一个控制器同时只能播放一个动画 */
	var currentAnimId: String?

	/** 当前动画影响的骨骼名称集合, 用于骨骼冲突检测. 空集合表示不参与冲突检测 */
	var affectedBones: Set<String>

	fun isActive(): Boolean

	/** 每帧驱动: 后端→ProxyModel + 过渡 */
	fun tick(partialTick: Float, deltaSec: Float)

	/** 触发动画, 传入过渡 tick 数 */
	fun trigger(animId: String, transitionTicks: Int)

	/** 触发动画, 指定过渡 tick 数和播放倍数 */
	fun trigger(animId: String, transitionTicks: Int, speedMultiplier: Float)

	/**
	 * 触发动画并使其在指定 tick 数内完成 (自动计算倍数).
	 * @param durationTicks 期望动画在多少个 tick 内完成
	 * @param originalAnimLengthSec 动画原始时长 (秒), 从 eyelib 动画元数据获取
	 */
	fun triggerForDuration(animId: String, transitionTicks: Int, durationTicks: Int, originalAnimLengthSec: Float)
	fun stop()
	fun stopAnimation(animId: String)
	fun restartAnimation(animId: String)
}
