package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimationPlayData
import net.neoforged.bus.api.Event

abstract class AnimationEvent(
	val animationController: IEntityAnimationController<*>,
) : Event() {
	fun getAnimationData(): AnimationPlayData {
		return animationController.currentConfig
	}
}