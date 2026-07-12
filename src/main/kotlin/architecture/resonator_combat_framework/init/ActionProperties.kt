package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.combat.AttackActionProperty
import architecture.resonator_combat_framework.combat.BooleanStateProperty
import architecture.resonator_combat_framework.combat.FloatStateProperty

/**
 * 预定义的 Action 状态修饰键对象。
 * 存储在 [Action.properties] 或 [AttackActionPhase.properties] 中，
 * 由 [AnimationAction.applyModifiers] 和 [AttackAnimationAction.applyPhaseModifiers] 读取并应用到 [EntityStateHolder]。
 */
object ActionProperties {
	/** 能否移动 */
	@JvmField
	val CAN_MOVE = BooleanStateProperty("can_move")

	/** 能否转动视角 */
	@JvmField
	val CAN_LOOK_AROUND = BooleanStateProperty("can_look_around")

	/** 能否切换物品 */
	@JvmField
	val CAN_SWITCH_ITEM = BooleanStateProperty("can_switch_item")

	/** 移动速度倍率 */
	@JvmField
	val SPEED_MODIFIER = FloatStateProperty("speed_modifier")

	/** 最大视角转动速度（弧度/秒） */
	@JvmField
	val MAX_LOOK_SPEED = FloatStateProperty("max_look_speed")

	/** 伤害倍率 */
	@JvmField
	val DAMAGE_MULTIPLIER = AttackActionProperty<Float>("damage_multiplier")
}