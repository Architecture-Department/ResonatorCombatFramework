package architecture.resonator_combat_framework.util

/**
 * 时间计算工具 —— 动画系统与战斗状态机共用相同的时间推进算法。
 *
 * 核心公式：`scaledDelta = rawDelta * speedMultiplier`
 *
 * - speedMultiplier > 0：正常/加速播放
 * - speedMultiplier < 0：倒放
 * - speedMultiplier = 0：暂停
 *
 * 动画系统在 [BedrockAnimationController.calcScaledDelta] 中额外追踪游戏刻 delta，
 * 最终缩放部分与此公式一致。
 */
object TimeUtil {

	/**
	 * 计算缩放后的时间增量，用于驱动动画/战斗的时间推进。
	 *
	 * @param rawDelta 原始时间增量（秒）
	 * @param speedMultiplier 速度倍率
	 * @return 缩放后的时间增量（秒）
	 */
	@JvmStatic
	fun calcScaledDelta(rawDelta: Float, speedMultiplier: Float): Float {
		return rawDelta * speedMultiplier
	}
}
