package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationProperty
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimationProperty

/**
 * [ActionAnimationDef] 及其子类的静态属性包装类型。
 *
 * 持有动画层面的泛型属性值，提供类型安全的属性访问。
 *
 * @param T 属性值的类型
 */
class ActionAnimationProperty<T>(name: String) : StaticAnimationProperty<T>(name)

/**
 * [AttackAnimationDef] 的静态属性包装类型。
 *
 * 继承自 [ActionAnimationProperty]，专用于攻击类动画的附加属性。
 */
class AttackAnimationProperty<T>(name: String) : ActionAnimationProperty<T>(name)

/**
 * 攻击阶段（[AttackPhase]）的属性包装类型。
 *
 * 独立于动画层次结构，用于为特定攻击阶段附加运行时属性（如伤害倍率）。
 */
class AttackPhaseProperty<T>(name: String) : AnimationProperty<T>(name)
