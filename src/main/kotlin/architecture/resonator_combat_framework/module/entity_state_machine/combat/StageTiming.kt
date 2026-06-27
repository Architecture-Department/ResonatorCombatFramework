package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.goldenboughs_lib.api.AllOpe
import kotlin.math.max

/**
 * 攻击段时序配置 —— 定义前摇/执行/后摇的时长。
 *
 * @param windupSec 前摇时长（秒）
 * @param activeSec 执行时长（秒）
 * @param recoverySec 后摇时长（秒）
 * @param resetSec 连击超时（秒，超过此值未连击则重置）
 */
@ConsistentCopyVisibility
@AllOpe
data class StageTiming
private constructor(
	val windupSec: Float,
	val activeSec: Float,
	val recoverySec: Float,
	val resetSec: Float,
) {

	companion object {
		@JvmStatic
		fun of(windup: Int = 0, active: Int = 4, recovery: Int = 2, reset: Int = 10): StageTiming {
			return of(windup / 20f, active / 20f, recovery / 20f, reset / 20f)
		}

		@JvmStatic
		fun of(windup: Float = 0.0f, active: Float = 0.2f, recovery: Float = 0.1f, reset: Float = 0.5f): StageTiming {
			return StageTiming(clamp(windup), clamp(active), clamp(recovery), clamp(reset))
		}

		@JvmStatic
		fun clamp(activeSec: Float): Float {
			return max(activeSec, 0f)
		}
	}
}