package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.module.entity_state_machine.combat.Action
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation

object RcfCombatActions {
	private val COMBAT_ACTIONS = mutableMapOf<ResourceLocation, Action>()

	@JvmStatic
	fun register(id: ResourceLocation, actions: Action) {
		COMBAT_ACTIONS[id] = actions
	}

	@JvmStatic
	private fun register(id: String, actions: Action) {
		register(RcfUtil.modRl(id), actions)
	}

	@JvmStatic
	fun get(id: ResourceLocation): Action? {
		return getCombatAction(id)
	}

	@JvmStatic
	fun getAll(): Map<ResourceLocation, Action> {
		return COMBAT_ACTIONS
	}

	@JvmStatic
	fun getCombatAction(id: ResourceLocation): Action? {
		return COMBAT_ACTIONS[id]
	}
}
