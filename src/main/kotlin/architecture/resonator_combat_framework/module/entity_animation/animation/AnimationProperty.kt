package architecture.resonator_combat_framework.module.entity_animation.animation

import architecture.goldenboughs_lib.api.AllOpe

/**
 * 动画属性键——类型安全的键，用于链式配置动画的运行时行为。
 *
 * 分层设计（对应 [StaticAnimation] 的继承层次）：
 * - [StaticAnimationProperty]：所有动画通用
 * - [ActionAnimationProperty]：动作动画（如持有物品）
 * - [AttackAnimationProperty]：攻击动画
 * - [AttackPhaseProperty]：攻击阶段（独立于动画层次）
 *
 * @param T 属性值类型
 * @param name 唯一名称（调试/序列化用）
 */
@AllOpe
class AnimationProperty<T>(val name: String)

/** 所有 [StaticAnimation] 通用的属性 */
class StaticAnimationProperty<T>(name: String) : AnimationProperty<T>(name)
