package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.goldenboughs_lib.api.AllOpe

/**
 * 打断配置 —— 定义攻击段各部分的可打断性及外部规则。
 *
 * 权重值含义：
 * - ≥ 0：可被打断，当 `打断权重 < 目标动作权重` 时打断成功
 * - -1：不可被打断
 *
 * 默认值：
 * - WINDUP 前摇: 5000
 * - ACTIVE 执行: 10000
 * - RECOVERY 后摇: 1000
 * - IDLE 空闲: 0（随时可打断）
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
	companion object {
		/** 默认打断配置（各阶段均使用默认权重） */
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
