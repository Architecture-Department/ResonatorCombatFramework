package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.animation.AttackAnimation
import architecture.resonator_combat_framework.init.RcfStaticAnimations
import architecture.resonator_combat_framework.module.entity_state_machine.combat.Action
import architecture.resonator_combat_framework.module.entity_state_machine.combat.InterruptData
import architecture.resonator_combat_framework.module.entity_state_machine.combat.StageTiming
import net.minecraft.resources.ResourceLocation

data class AnimationAction(
	override val id: ResourceLocation,
	val animationId: ResourceLocation,
	override val timing: StageTiming,
	override val interruptData: InterruptData = InterruptData(),
	override val weight: Int
) : Action(
	id,
	timing,
	interruptData,
	weight
) {

	fun getAnimation(isClient: Boolean): AttackAnimation {
		return RcfStaticAnimations.getStaticAnimation(isClient, animationId) as? AttackAnimation
			?: throw IllegalArgumentException("AnimationStageConfig: animationId is not an AttackAnimation")
	}

	fun verify(isClient: Boolean) {
		getAnimation(isClient)
	}
}