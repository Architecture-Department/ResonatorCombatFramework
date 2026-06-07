package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.resonator_combat_framework.module.entity_animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.mapper.IEntityAnimationMapper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

abstract class AnimationControllerEvent<T : Entity>(
	val id: ResourceLocation,
	val animaController: IEntityAnimationController<T>,
	val animaMapper: IEntityAnimationMapper<T, *>
) : Event() {

	class TickHandlerPre<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		animaMapper: IEntityAnimationMapper<T, *>
	) : AnimationControllerEvent<T>(id, animaController, animaMapper), ICancellableEvent

	class TickHandlerPost<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		animaMapper: IEntityAnimationMapper<T, *>
	) : AnimationControllerEvent<T>(id, animaController, animaMapper)

	class TickPre<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		animaMapper: IEntityAnimationMapper<T, *>
	) : AnimationControllerEvent<T>(id, animaController, animaMapper), ICancellableEvent

	class TickPost<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		animaMapper: IEntityAnimationMapper<T, *>
	) : AnimationControllerEvent<T>(id, animaController, animaMapper)
}