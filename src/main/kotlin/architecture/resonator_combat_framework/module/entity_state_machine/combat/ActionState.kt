package architecture.resonator_combat_framework.module.entity_state_machine.combat

/**
 * 当前攻击所处的阶段。
 */
enum class ActionState {
	EMPTY,

	/** 前摇 */
	WINDUP,

	/** 执行 */
	ACTIVE,

	/** 后摇 */
	RECOVERY,

	/** 空闲 */
	IDLE,
}