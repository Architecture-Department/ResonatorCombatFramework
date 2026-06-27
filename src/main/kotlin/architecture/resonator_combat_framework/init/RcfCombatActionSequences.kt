package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionSequence
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation

object RcfCombatActionSequences {
	private val COMBAT_ACTION_SEQUENCE = mutableMapOf<ResourceLocation, ActionSequence>()

	@JvmStatic
	fun register(id: ResourceLocation, actionSequence: ActionSequence) {
		COMBAT_ACTION_SEQUENCE[id] = actionSequence
	}

	@JvmStatic
	private fun register(id: String, actionSequence: ActionSequence) {
		register(RcfUtil.modRl(id), actionSequence)
	}

	@JvmStatic
	fun get(id: ResourceLocation): ActionSequence? {
		return getCombatActions(id)
	}

	@JvmStatic
	fun getAll(): Map<ResourceLocation, ActionSequence> {
		return COMBAT_ACTION_SEQUENCE
	}

	@JvmStatic
	fun getCombatActions(id: ResourceLocation): ActionSequence? {
		return COMBAT_ACTION_SEQUENCE[id]
	}
}
