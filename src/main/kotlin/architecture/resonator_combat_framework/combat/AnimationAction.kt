package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.animation.AttackAnimation
import architecture.resonator_combat_framework.module.entity_animation.IProxyAnimationProvider
import architecture.resonator_combat_framework.module.entity_animation.IProxyAnimationProvider.Companion.getAnimationTransformer
import architecture.resonator_combat_framework.module.entity_animation.registry.StaticAnimationRegistry
import architecture.resonator_combat_framework.module.entity_state_machine.combat.Action
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionSequence
import architecture.resonator_combat_framework.module.entity_state_machine.combat.InterruptData
import architecture.resonator_combat_framework.module.entity_state_machine.combat.StageTiming
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

class AnimationAction(
	override val id: ResourceLocation,
	val animationId: ResourceLocation,
	override val timing: StageTiming,
	override val interruptData: InterruptData = InterruptData(),
	override val weight: Int,
	val controllerName: ResourceLocation?,
) : Action(id, timing, interruptData, weight) {

	fun getAnimation(): AttackAnimation {
		val holder = StaticAnimationRegistry.find(animationId)
			?: throw IllegalArgumentException("AnimationAction: AttackAnimation not found: $animationId")
		return holder.get() as? AttackAnimation
			?: throw IllegalArgumentException("AnimationAction: $animationId is not an AttackAnimation")
	}

	fun verify(isClient: Boolean) {
		getAnimation()
	}

	override fun onStart(entity: LivingEntity, actionSequence: ActionSequence?) {
		super.onStart(entity, actionSequence)
		if (entity is IProxyAnimationProvider) {
			entity.getAnimationTransformer().getController(controllerName)?.trigger(getAnimation())
		}
	}

}
