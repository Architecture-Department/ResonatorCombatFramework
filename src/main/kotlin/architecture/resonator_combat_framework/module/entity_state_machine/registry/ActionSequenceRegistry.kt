package architecture.resonator_combat_framework.module.entity_state_machine.registry

import architecture.goldenboughs_lib.api.AllOpe
import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionSequence
import architecture.resonator_combat_framework.module.entity_state_machine.event.ActionSequenceRegistryEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

@AllOpe
class ActionSequenceRegistry :
	SimplePreparableReloadListener<Map<ResourceLocation, LazySupplier<ActionSequence>>>() {
	companion object {
		private val INSTANCE: ActionSequenceRegistry = ActionSequenceRegistry()

		@JvmStatic
		fun getInstance(): ActionSequenceRegistry {
			return INSTANCE
		}

		@JvmStatic
		fun find(id: ResourceLocation): LazySupplier<ActionSequence>? {
			return getInstance().get(id)
		}
	}

	private val actionSequences = mutableMapOf<ResourceLocation, LazySupplier<ActionSequence>>()

	fun get(id: ResourceLocation): LazySupplier<ActionSequence>? = actionSequences[id]

	override fun prepare(
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	): Map<ResourceLocation, LazySupplier<ActionSequence>> {
		return FORGE_BUS.post(ActionSequenceRegistryEvent()).getAll()
	}

	override fun apply(
		map: Map<ResourceLocation, LazySupplier<ActionSequence>>,
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	) {
		actionSequences.clear()
		actionSequences.putAll(map)
		actionSequences.forEach {
			it.value.init()
		}
	}
}