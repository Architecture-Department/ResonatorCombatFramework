package architecture.resonator_combat_framework.module.state_machine.event

import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.combat.ActionSequence
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.Event

/**
 * [ActionSequence] 注册事件 —— 在资源重载时由 [ActionSequenceRegistry] 触发，
 * 用于收集所有注册的 ActionSequence。
 * [ActionSequence]注册事件
 */
class ActionSequenceRegisterEvent : Event() {
	private val map = linkedMapOf<ResourceLocation, LazySupplier<ActionSequence>>()

	fun <T : ActionSequence> register(
		id: ResourceLocation, function: (id: ResourceLocation) -> T
	): LazySupplier<T> {
		@Suppress("RemoveExplicitTypeArguments") val supplier = LazySupplier<T> { function(id) }
		@Suppress("UNCHECKED_CAST")
		map[id] = supplier as LazySupplier<ActionSequence>
		return supplier
	}

	fun getAll(): Map<ResourceLocation, LazySupplier<ActionSequence>> = map
}
