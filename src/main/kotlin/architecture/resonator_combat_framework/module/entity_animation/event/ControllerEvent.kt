package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapperProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

@AllOpe
abstract class ControllerEvent<T : Entity>(
	val id: ResourceLocation,
	val animaController: IEntityAnimationController<T>,
	val animaMapper: IEntityAnimationMapperProvider<T, *>
) : Event() {

	class TickHandlerPre<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		animaMapper: IEntityAnimationMapperProvider<T, *>
	) : ControllerEvent<T>(id, animaController, animaMapper), ICancellableEvent

	class TickHandlerPost<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		animaMapper: IEntityAnimationMapperProvider<T, *>
	) : ControllerEvent<T>(id, animaController, animaMapper)

	class TickPre<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		animaMapper: IEntityAnimationMapperProvider<T, *>
	) : ControllerEvent<T>(id, animaController, animaMapper), ICancellableEvent

	class TickPost<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		animaMapper: IEntityAnimationMapperProvider<T, *>
	) : ControllerEvent<T>(id, animaController, animaMapper)
}