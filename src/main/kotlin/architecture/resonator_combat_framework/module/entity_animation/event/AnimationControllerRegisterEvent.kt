package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.resonator_combat_framework.module.entity_animation.animation.controller.BedrockAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.AnimationControllerManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event

/**
 * 动画控制器注册事件
 */
class AnimationControllerRegisterEvent<T : Entity> : Event() {
	private val entries = mutableMapOf<ResourceLocation, ControllerEntry<T>>()

	/**
	 * 注册动画 ID 到控制器的映射。
	 * @param id 目标控制器名称
	 * @param priority 排序值（越大越优先添加，默认 1000）
	 */
	@JvmOverloads
	fun register(
		id: ResourceLocation,
		controllerFactory: (manager: AnimationControllerManager<T>, id: ResourceLocation, isClient: Boolean) -> IEntityAnimationController<T> = { manager, id, isClient ->
			BedrockAnimationController(manager, id, isClient)
		},
		priority: Int
	) {
		entries[id] = ControllerEntry(id, controllerFactory, priority)
	}

	fun register(
		id: ResourceLocation,
		controllerFactory: (manager: AnimationControllerManager<T>, isClient: Boolean) -> IEntityAnimationController<T>,
		priority: Int
	) {
		register(id, { manager, id, isClient -> controllerFactory(manager, isClient) }, priority)
	}

	@JvmRecord
	data class ControllerEntry<T : Entity>(
		val controllerName: ResourceLocation,
		val controllerFactory: (manager: AnimationControllerManager<T>, id: ResourceLocation, isClient: Boolean) -> IEntityAnimationController<T>,
		val priority: Int
	)

	/** 按 order 升序返回所有注册项 */
	fun getSortedEntries(): List<ControllerEntry<T>> = entries.values.sortedBy { it.priority }
}

