package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.goldenboughs_lib.api.AllOpe

/**
 * 打断配置 —— 定义攻击段各部分的可打断性及外部规则。
 * 如果定义为-1则不可能被打断
 *
 * @constructor Creates a new InterruptData
 * @property windup 前摇
 * @property attack 攻击
 * @property recovery 后摇
 * @property idle 空闲
 */
@AllOpe
data class InterruptData(
	val windup: Int = 5000,
	val attack: Int = 10000,
	val recovery: Int = 1000,
	val idle: Int = 0
) {
	companion object{
		@JvmField
		val DEFAULT = InterruptData()
	}

	val interruptions: Map<ActionState, Int> = mapOf(
		ActionState.WINDUP to windup,
		ActionState.ACTIVE to attack,
		ActionState.RECOVERY to recovery,
		ActionState.IDLE to idle,
	)

	fun getInterruptWeight(actionState: ActionState): Int {
		return interruptions[actionState] ?: 0
	}

	override fun toString(): String {
		return "InterruptData(" +
			"attack=$attack, " +
			"windup=$windup, " +
			"recovery=$recovery, " +
			"idle=$idle)"
	}
}