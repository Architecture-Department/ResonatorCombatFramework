package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapperProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

/**
 * 控制器阶段事件，提供动画控制器在 tick 处理前后触发的生命周期事件。
 * 包含 TickHandler 阶段（整体控制器处理）和 Tick 阶段（单次 tick 动画更新）的前后事件。
 */
@AllOpe
abstract class ControllerEvent<T : Entity>(
	val id: ResourceLocation,
	val animaController: IEntityAnimationController<T>,
	val animaMapper: IEntityAnimationMapperProvider<T, *>
) : Event() {

	/**
	 * 控制器处理前事件 —— 在 [IEntityAnimationMapperProvider.handler] 的 tick 处理开始前触发。
	 * 可取消，取消后将跳过本次 tick 的所有动画处理。
	 */
	class TickHandlerPre<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		animaMapper: IEntityAnimationMapperProvider<T, *>
	) : ControllerEvent<T>(id, animaController, animaMapper), ICancellableEvent

	/**
	 * 控制器处理后事件 —— 在 [IEntityAnimationMapperProvider.handler] 的 tick 处理结束后触发。
	 */
	class TickHandlerPost<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		animaMapper: IEntityAnimationMapperProvider<T, *>
	) : ControllerEvent<T>(id, animaController, animaMapper)

	/**
	 * Tick 前事件 —— 在单个动画控制器的 tick 更新开始前触发。
	 * 可取消，取消后将跳过该控制器的本次 tick 更新。
	 */
	class TickPre<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		animaMapper: IEntityAnimationMapperProvider<T, *>
	) : ControllerEvent<T>(id, animaController, animaMapper), ICancellableEvent

	/**
	 * Tick 后事件 —— 在单个动画控制器的 tick 更新结束后触发。
	 */
	class TickPost<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		animaMapper: IEntityAnimationMapperProvider<T, *>
	) : ControllerEvent<T>(id, animaController, animaMapper)
}
