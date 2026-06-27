package architecture.resonator_combat_framework.init

import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.entity_state_machine.combat.Action
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation

object RcfCombatActions {
	private val COMBAT_ACTIONS = mutableMapOf<ResourceLocation, LazySupplier<Action>>()

	@JvmStatic
	fun register(id: ResourceLocation, actions: Action) {
		COMBAT_ACTIONS[id] = LazySupplier { actions }
	}

	@JvmStatic
	private fun register(id: String, actions: Action) {
		register(RcfUtil.modRl(id), actions)
	}

	@JvmStatic
	fun get(id: ResourceLocation): Action? {
		return COMBAT_ACTIONS[id]?.get()
	}

	@JvmStatic
	fun getAll(): Map<ResourceLocation, LazySupplier<Action>> {
		return COMBAT_ACTIONS
	}
}
