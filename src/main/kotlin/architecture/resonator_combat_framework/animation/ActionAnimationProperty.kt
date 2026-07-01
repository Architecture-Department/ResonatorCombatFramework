package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationProperty
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimationProperty

/** [ActionAnimationDef] 及其子类的属性 */
class ActionAnimationProperty<T>(name: String) : StaticAnimationProperty<T>(name)

/** [AttackAnimationDef] 的属性 */
class AttackAnimationProperty<T>(name: String) : ActionAnimationProperty<T>(name)

/** 攻击阶段（[AttackPhase]）的属性，独立于动画层次 */
class AttackPhaseProperty<T>(name: String) : AnimationProperty<T>(name)