package architecture.resonator_combat_framework.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.mapper.IEntityAnimationMapperProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

/**
 * 控制器阶段事件，提供动画控制器在 tick 处理前后触发的生命周期事件。
 * 包含 TickHandler 阶段（整体控制器处理）和 Tick 阶段（单次 tick 动画更新）的前后事件。
 */
@AllOpe
abstract class AnimationControllerEvent<T : Entity>(
	val id: ResourceLocation,
	val controller: IEntityAnimationController<T>,
	val mapperProvider: IEntityAnimationMapperProvider<T, *>
) : Event() {

	class Tick<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		mapperProvider: IEntityAnimationMapperProvider<T, *>
	) : AnimationControllerEvent<T>(id, animaController, mapperProvider) {
		/**
		 * Tick 前事件 —— 在单个动画控制器的 tick 更新开始前触发。
		 * 可取消
		 */
		class Pre<T : Entity>(
			id: ResourceLocation,
			animaController: IEntityAnimationController<T>,
			mapperProvider: IEntityAnimationMapperProvider<T, *>
		) : Tick<T>(id, animaController, mapperProvider), ICancellableEvent

		/**
		 * Tick 后事件 —— 在单个动画控制器的 tick 更新结束后触发。
		 */
		class Post<T : Entity>(
			id: ResourceLocation,
			animaController: IEntityAnimationController<T>,
			mapperProvider: IEntityAnimationMapperProvider<T, *>
		) : Tick<T>(id, animaController, mapperProvider)
	}

	class HandlerTick<T : Entity>(
		id: ResourceLocation,
		animaController: IEntityAnimationController<T>,
		mapperProvider: IEntityAnimationMapperProvider<T, *>
	) : AnimationControllerEvent<T>(id, animaController, mapperProvider) {
		/**
		 * 控制器处理前事件 —— 在 [IEntityAnimationMapperProvider.handler] 的 tick 处理开始前触发。
		 * 可取消，取消后将跳过本次 tick 的所有动画处理。
		 */
		class Pre<T : Entity>(
			id: ResourceLocation,
			animaController: IEntityAnimationController<T>,
			mapperProvider: IEntityAnimationMapperProvider<T, *>
		) : HandlerTick<T>(id, animaController, mapperProvider), ICancellableEvent

		/**
		 * 控制器处理后事件 —— 在 [IEntityAnimationMapperProvider.handler] 的 tick 处理结束后触发。
		 */
		class Post<T : Entity>(
			id: ResourceLocation,
			animaController: IEntityAnimationController<T>,
			mapperProvider: IEntityAnimationMapperProvider<T, *>
		) : HandlerTick<T>(id, animaController, mapperProvider)
	}
}
