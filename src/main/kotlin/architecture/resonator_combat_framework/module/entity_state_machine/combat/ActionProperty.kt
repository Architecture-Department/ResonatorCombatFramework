package architecture.resonator_combat_framework.module.entity_state_machine.combat

/**
 * 动作属性键——类型安全的键，用于在 Action 系统中配置运行时行为参数。
 *
 * @param T 属性值类型
 * @param name 唯一名称（调试/序列化用）
 */
open class ActionProperty<T>(val name: String)

/**
 * 攻击动作属性包装类型。
 */
class AttackActionProperty<T>(name: String) : ActionProperty<T>(name)
