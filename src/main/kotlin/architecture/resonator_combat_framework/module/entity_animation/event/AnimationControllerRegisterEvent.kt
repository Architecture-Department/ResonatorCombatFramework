package architecture.resonator_combat_framework.module.entity_animation.event
import architecture.resonator_combat_framework.module.entity_animation.controller.BedrockAnimationController
import architecture.resonator_combat_framework.module.entity_animation.controller.IAnimationController
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.Event

/**
 * 动画控制器注册事件
 */
class AnimationControllerRegisterEvent : Event() {
	private val entries = mutableMapOf<ResourceLocation, ControllerEntry>()

	/**
	 * 注册动画 ID 到控制器的映射。
	 * @param id 目标控制器名称
	 * @param priority 排序值（越大越优先添加，默认 1000）
	 */
	@JvmOverloads
	fun register(
		id: ResourceLocation,
		controllerFactory: (id: ResourceLocation, isClient: Boolean) -> IAnimationController = { id, isClient ->
			BedrockAnimationController(id, isClient)
		},
		priority: Int
	) {
		entries[id] = ControllerEntry(id, controllerFactory, priority)
	}

	fun register(
		id: ResourceLocation,
		controllerFactory: (isClient: Boolean) -> IAnimationController,
		priority: Int
	) {
		register(id, { id, isClient -> controllerFactory(isClient) }, priority)
	}

	@JvmRecord
	data class ControllerEntry(
		val controllerName: ResourceLocation,
		val controllerFactory: (id: ResourceLocation, isClient: Boolean) -> IAnimationController,
		val priority: Int
	)

	/** 按 order 升序返回所有注册项 */
	fun getSortedEntries(): List<ControllerEntry> = entries.values.sortedByDescending { it.priority }
}

