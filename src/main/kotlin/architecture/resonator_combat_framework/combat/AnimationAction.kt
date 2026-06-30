package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.module.entity_animation.IProxyAnimationProvider
import architecture.resonator_combat_framework.module.entity_animation.IProxyAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_state_machine.combat.Action
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionSequence
import architecture.resonator_combat_framework.module.entity_state_machine.combat.InterruptData
import architecture.resonator_combat_framework.module.entity_state_machine.combat.StageTiming
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.util.function.Supplier

class AnimationAction<T : StaticAnimation>
@JvmOverloads
constructor(
	id: ResourceLocation,
	val animation: Supplier<T?>,
	val controllerName: ResourceLocation?,
	timing: StageTiming,
	interruptData: InterruptData = InterruptData(),
	override val weight: Int = 2500,
) : Action(id, timing, interruptData, weight) {

	fun getAnimation(): T {
		val animation = animation.get()
		animation ?: throw IllegalArgumentException("AnimationAction: AttackAnimation not found: ${this.animation}")
		return animation
	}

	override fun onStart(entity: LivingEntity, actionSequence: ActionSequence?) {
		super.onStart(entity, actionSequence)
		if (entity is IProxyAnimationProvider) {
			getController(entity)?.trigger(getAnimation())
		}
	}

	override fun onEnd(entity: LivingEntity, actionSequence: ActionSequence?) {
		super.onEnd(entity, actionSequence)
		if (entity is IProxyAnimationProvider) {
			getController(entity)?.stop()
		}
	}

	override fun onSpeedModify(entity: LivingEntity, actionSequence: ActionSequence?, oldValue: Float, newValue: Float) {
		super.onSpeedModify(entity, actionSequence, oldValue, newValue)
		if (entity is IProxyAnimationProvider) {
			getController(entity)?.speedMultiplier = newValue
		}
	}

	protected fun getController(entity: IProxyAnimationProvider): IEntityAnimationController<out Entity>? {
		return entity.getMapperProvider().getController(controllerName)
	}
}
