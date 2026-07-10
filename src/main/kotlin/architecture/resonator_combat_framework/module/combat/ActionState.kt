package architecture.resonator_combat_framework.module.combat

/**
 * 动作阶段枚举 —— 描述一次攻击动作中实体所处的不同时间窗口。
 *
 * 各阶段按时间顺序：WINDUP（前摇）→ ACTIVE（执行）→ RECOVERY（后摇）→ IDLE（空闲）。
 * EMPTY 表示无动作状态，由 [ActionController] 在动作结束时设置。
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
