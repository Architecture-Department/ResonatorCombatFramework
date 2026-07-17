package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.util.RcfUtil

object RcfEntityStates {
	/** 移动中（速度超过阈值且脚步动画中） */
	@JvmField
	val MOVE_STATE = RcfUtil.modRl("move")

	/** 吃食物中 */
	@JvmField
	val EATING_STATE = RcfUtil.modRl("eating")

	/** 能否移动 */
	@JvmField
	val CAN_MOVE = RcfUtil.modRl("can_move")

	/** 能否转动视角 */
	@JvmField
	val CAN_LOOK_AROUND = RcfUtil.modRl("can_look_around")

	/** 移动速度倍率（0=不能移动，1=正常速度），float 状态 */
	@JvmField
	val SPEED_MODIFIER = RcfUtil.modRl("speed_modifier")

	/** 最大视角转动速度（弧度/秒），float 状态 */
	@JvmField
	val MAX_LOOK_SPEED = RcfUtil.modRl("max_look_speed")

	/** 能否切换物品 */
	@JvmField
	val CAN_SWITCH_ITEM = RcfUtil.modRl("can_switch_item")
}