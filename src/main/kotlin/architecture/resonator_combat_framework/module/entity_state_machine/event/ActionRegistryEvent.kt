package architecture.resonator_combat_framework.module.entity_state_machine.event

import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.entity_state_machine.combat.Action
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.Event

/**
 * [Action] 注册事件 —— 在资源重载时由 [ActionRegistry] 触发，
 * 用于收集所有注册的 Action。
 * [Action]注册事件
 */
class ActionRegistryEvent : Event() {
	private val map = linkedMapOf<ResourceLocation, LazySupplier<Action>>()

	fun <T : Action> register(
		id: ResourceLocation, function: (id: ResourceLocation) -> T
	): LazySupplier<T> {
		@Suppress("RemoveExplicitTypeArguments") val supplier = LazySupplier<T> { function(id) }
		@Suppress("UNCHECKED_CAST")
		map[id] = supplier as LazySupplier<Action>
		return supplier
	}

	fun getAll(): Map<ResourceLocation, LazySupplier<Action>> = map
}
