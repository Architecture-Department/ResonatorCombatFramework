package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.module.combat.ActionProperties

/**
 * 动画属性定义  —— 委托到 [ActionProperties]。
 * 保留此类以兼容旧引用。
 */
object AnimationProperties {
	@JvmField
	val DAMAGE_MULTIPLIER = ActionProperties.DAMAGE_MULTIPLIER
}
