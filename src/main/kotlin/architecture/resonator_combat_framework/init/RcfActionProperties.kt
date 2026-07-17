package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.combat.AttackActionProperty
import architecture.resonator_combat_framework.combat.BooleanStateProperty
import architecture.resonator_combat_framework.combat.FloatStateProperty
import architecture.resonator_combat_framework.util.RcfUtil.modRl

object RcfActionProperties {
	/** 能否移动 */
	@JvmField
	val CAN_MOVE = BooleanStateProperty(modRl("can_move"))

	/** 能否转动视角 */
	@JvmField
	val CAN_LOOK_AROUND = BooleanStateProperty(modRl("can_look_around"))

	/** 能否切换物品 */
	@JvmField
	val CAN_SWITCH_ITEM = BooleanStateProperty(modRl("can_switch_item"))

	/** 移动速度倍率 */
	@JvmField
	val SPEED_MODIFIER = FloatStateProperty(modRl("speed_modifier"))

	/** 最大视角转动速度（弧度/秒） */
	@JvmField
	val MAX_LOOK_SPEED = FloatStateProperty(modRl("max_look_speed"))

	/** 伤害倍率 */
	@JvmField
	val DAMAGE_MULTIPLIER = AttackActionProperty<Float>(modRl("damage_multiplier"))
}