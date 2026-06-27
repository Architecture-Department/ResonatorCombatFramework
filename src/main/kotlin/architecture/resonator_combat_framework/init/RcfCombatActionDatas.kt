package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionData
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation

object RcfCombatActionDatas {
	private val COMBAT_ACTION_DATAS = mutableMapOf<ResourceLocation, ActionData>()

	@JvmStatic
	fun register(id: ResourceLocation, actionData: ActionData) {
		COMBAT_ACTION_DATAS[id] = actionData
	}

	@JvmStatic
	private fun register(id: String, actionData: ActionData) {
		register(RcfUtil.modRl(id), actionData)
	}

	@JvmStatic
	fun get(id: ResourceLocation): ActionData? {
		return getCombatActions(id)
	}

	@JvmStatic
	fun getAll(): Map<ResourceLocation, ActionData> {
		return COMBAT_ACTION_DATAS
	}

	@JvmStatic
	fun getCombatActions(id: ResourceLocation): ActionData? {
		return COMBAT_ACTION_DATAS[id]
	}
}
