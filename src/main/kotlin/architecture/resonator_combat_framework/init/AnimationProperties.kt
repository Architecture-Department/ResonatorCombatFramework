package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.animation.AttackAnimationProperty
import org.jetbrains.annotations.NotNull

/**
 * 动画属性定义 —— 持有 [AttackAnimationProperty] 实例，
 * 供动画系统在运行时读取和修改动画行为属性。
 */
/**
 * 动画属性定义 —— 持有 [AttackAnimationProperty] 实例，
 * 供动画系统在运行时读取和修改动画行为属性。
 */
object AnimationProperties {
	@JvmField
	val DAMAGE_MULTIPLIER = AttackAnimationProperty<@NotNull Float>(
		"damage_multiplier"
	)
}
