package architecture.resonator_combat_framework.module.entity_state_machine.registry

import architecture.goldenboughs_lib.api.AllOpe
import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.entity_state_machine.combat.Action
import architecture.resonator_combat_framework.module.entity_state_machine.event.ActionRegistryEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

@AllOpe
class ActionRegistry :
	SimplePreparableReloadListener<Map<ResourceLocation, LazySupplier<Action>>>() {
	companion object {
		private val INSTANCE: ActionRegistry = ActionRegistry()

		@JvmStatic
		fun getInstance(): ActionRegistry {
			return INSTANCE
		}

		@JvmStatic
		fun find(id: ResourceLocation): LazySupplier<Action>? {
			return getInstance().get(id)
		}
	}

	private val actions = mutableMapOf<ResourceLocation, LazySupplier<Action>>()

	fun get(id: ResourceLocation): LazySupplier<Action>? = actions[id]

	override fun prepare(
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	): Map<ResourceLocation, LazySupplier<Action>> {
		return FORGE_BUS.post(ActionRegistryEvent()).getAll()
	}

	override fun apply(
		map: Map<ResourceLocation, LazySupplier<Action>>,
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	) {
		actions.clear()
		actions.putAll(map)
		actions.forEach {
			it.value.init()
		}
	}
}