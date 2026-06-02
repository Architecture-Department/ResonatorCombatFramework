package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.resonator_combat_framework.module.entity_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.entity_animation.controller.IAnimationController
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

abstract class AnimationControllerEvent(
	val id: ResourceLocation,
	val animaController: IAnimationController,
	val animaMapper: IAnimationMapper
) : Event() {
	class TickHandlerPre(
		id: ResourceLocation,
		animaController: IAnimationController,
		animaMapper: IAnimationMapper
	) : AnimationControllerEvent(id, animaController, animaMapper), ICancellableEvent

	class TickHandlerPost(
		id: ResourceLocation,
		animaController: IAnimationController,
		animaMapper: IAnimationMapper
	) : AnimationControllerEvent(id, animaController, animaMapper) {

	}

	class TickPre(
		id: ResourceLocation,
		animaController: IAnimationController,
		animaMapper: IAnimationMapper
	) : AnimationControllerEvent(id, animaController, animaMapper), ICancellableEvent

	class TickPost(
		id: ResourceLocation,
		animaController: IAnimationController,
		animaMapper: IAnimationMapper
	) : AnimationControllerEvent(id, animaController, animaMapper) {

	}
}