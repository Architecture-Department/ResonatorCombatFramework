package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.animation.AttackAnimationProperty
import org.jetbrains.annotations.NotNull

object AnimationProperties {
	@JvmField
	val DAMAGE_MULTIPLIER = AttackAnimationProperty<@NotNull Float>(
		"damage_multiplier"
	)
}
