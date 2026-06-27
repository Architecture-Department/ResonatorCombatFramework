package architecture.resonator_combat_framework.init

import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionSequence
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation

object RcfCombatActionSequences {
	private val COMBAT_ACTION_SEQUENCE = mutableMapOf<ResourceLocation, LazySupplier<ActionSequence>>()

	@JvmStatic
	fun register(id: ResourceLocation, actionSequence: ActionSequence) {
		COMBAT_ACTION_SEQUENCE[id] = LazySupplier { actionSequence }
	}

	@JvmStatic
	private fun register(id: String, actionSequence: ActionSequence) {
		register(RcfUtil.modRl(id), actionSequence)
	}

	@JvmStatic
	fun get(id: ResourceLocation): ActionSequence? {
		return COMBAT_ACTION_SEQUENCE[id]?.get()
	}

	@JvmStatic
	fun getAll(): Map<ResourceLocation, LazySupplier<ActionSequence>> {
		return COMBAT_ACTION_SEQUENCE
	}
}
