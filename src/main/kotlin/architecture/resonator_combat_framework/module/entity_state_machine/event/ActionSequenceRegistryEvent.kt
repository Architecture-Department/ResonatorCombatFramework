package architecture.resonator_combat_framework.module.entity_state_machine.event

import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionSequence
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.Event

/**
 * [ActionSequence]注册事件
 */
class ActionSequenceRegistryEvent : Event() {
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