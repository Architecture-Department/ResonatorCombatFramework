package architecture.resonator_combat_framework.module.animation.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.animation.controller.AnimationController
import architecture.resonator_combat_framework.module.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.animation.mapper.AnimationControllerManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event

/**
 * 动画控制器注册事件，用于将动画 ID 与控制器工厂注册到动画映射器中。
 * 支持按优先级排序，优先级越高的控制器越早被添加。
 */
@AllOpe
class ControllerRegisterEvent<T : Entity> : Event() {
	private val entries = mutableMapOf<ResourceLocation, ControllerEntry<T>>()

	/**
	 * 注册动画 ID 到控制器的映射。
	 * @param id 目标控制器的唯一标识
	 * @param controllerFactory 控制器工厂函数，接收管理器、ID 和客户端标记，返回控制器实例（默认使用 [AnimationController]）
	 * @param priority 排序值（数值越大越优先添加，默认 1000）
	 */
	@JvmOverloads
	fun register(
		id: ResourceLocation,
		controllerFactory: (manager: AnimationControllerManager<T>, id: ResourceLocation, isClient: Boolean) -> IEntityAnimationController<T> = { manager, id, isClient ->
			AnimationController(manager, id, isClient)
		},
		priority: Int
	) {
		entries[id] = ControllerEntry(id, controllerFactory, priority)
	}

	/**
	 * 简化注册方法（无需在工厂函数中显式接收 ID）。
	 * @param id 目标控制器的唯一标识
	 * @param controllerFactory 控制器工厂函数，接收管理器和客户端标记，返回控制器实例
	 * @param priority 排序值（数值越大越优先添加）
	 */
	fun register(
		id: ResourceLocation,
		controllerFactory: (manager: AnimationControllerManager<T>, isClient: Boolean) -> IEntityAnimationController<T>,
		priority: Int
	) {
		register(id, { manager, id, isClient -> controllerFactory(manager, isClient) }, priority)
	}

	/**
	 * 控制器注册条目，包含控制器 ID、工厂函数和优先级。
	 * @property controllerName 控制器唯一标识
	 * @property controllerFactory 用于创建控制器实例的工厂函数
	 * @property priority 排序优先级（数值越大越优先）
	 */
	@JvmRecord
	data class ControllerEntry<T : Entity>(
		val controllerName: ResourceLocation,
		val controllerFactory: (manager: AnimationControllerManager<T>, id: ResourceLocation, isClient: Boolean) -> IEntityAnimationController<T>,
		val priority: Int
	)

	/**
	 * 按优先级（升序）返回所有已注册的控制器条目。
	 * @return 排序后的 [ControllerEntry] 列表
	 */
	fun getSortedEntries(): List<ControllerEntry<T>> = entries.values.sortedBy { it.priority }
}
