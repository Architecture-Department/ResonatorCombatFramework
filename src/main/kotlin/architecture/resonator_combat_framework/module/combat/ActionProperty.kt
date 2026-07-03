package architecture.resonator_combat_framework.module.combat

import architecture.goldenboughs_lib.api.AllOpe

/**
 * 动作属性键——类型安全的键，用于在 Action 系统中配置运行时行为参数。
 *
 * 状态修饰（[BooleanStateProperty]/[FloatStateProperty]）同样是 [ActionProperty] 的子类，
 * 存储在 [Action] 或 [architecture.resonator_combat_framework.combat.AttackActionPhase] 的 properties 映射中。
 * 在应用到 [EntityStateHolder] 时，通过 [RcfUtil.modRl] 将属性名转换为 [ResourceLocation]。
 *
 * @param T 属性值类型
 * @param name 唯一名称（调试/序列化用，同时也是 EntityStateHolder 中使用的路径名）
 */
@AllOpe
class ActionProperty<T>(val name: String) {
	override fun toString(): String = "ActionProperty($name)"
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ActionProperty<*>) return false

		if (name != other.name) return false

		return true
	}

	override fun hashCode(): Int {
		return name.hashCode()
	}
}

/**
 * 攻击动作属性包装类型。
 */
class AttackActionProperty<T>(name: String) : ActionProperty<T>(name)

/**
 * 布尔状态修饰属性——用于 [EntityStateHolder] 的布尔状态键。
 */
class BooleanStateProperty(name: String) : ActionProperty<Boolean>(name)

/**
 * 浮点状态修饰属性——用于 [EntityStateHolder] 的浮点状态键。
 */
class FloatStateProperty(name: String) : ActionProperty<Float>(name)

/**
 * 预定义的 Action 状态修饰键对象。
 * 存储在 [Action.properties] 或 [architecture.resonator_combat_framework.combat.AttackActionPhase.properties] 中，
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
