package architecture.resonator_combat_framework.module.player_animation.event

// 动画控制器注册事件。NeoForge 事件，用于注册各部位的动画控制器到 AnimationController 管理器

import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController
import net.neoforged.bus.api.Event

/**
 * 动画控制器注册事件
 */
class AnimationControllerRegisterEvent : Event() {
	private val entries = mutableMapOf<String, ControllerEntry>()

	/**
	 * 注册动画 ID 到控制器的映射。
	 * @param controllerName 目标控制器名称
	 * @param order 排序值（越大越优先添加，默认 1000）
	 */
	fun register(
		controllerName: String,
		controllerFactory: (isClient: Boolean) -> IAnimationController,
		order: Int = 1000
	) {
		entries[controllerName] = ControllerEntry(controllerName, controllerFactory, order)
	}

	@JvmRecord
	data class ControllerEntry(
		val controllerName: String,
		val controllerFactory: (isClient: Boolean) -> IAnimationController,
		val priority: Int
	)

	/** 按 order 升序返回所有注册项 */
	fun getSortedEntries(): List<ControllerEntry> = entries.values.sortedBy { it.priority }
}
