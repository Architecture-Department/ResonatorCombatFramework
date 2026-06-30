package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.goldenboughs_lib.api.AllOpe
import net.minecraft.resources.ResourceLocation

/**
 * 动作序列
 */
@AllOpe
data class ActionSequence(
	val id: ResourceLocation,
	val stages: Array<Action>,
) {
	companion object {
		@JvmStatic
		fun of(
			id: ResourceLocation,
			vararg stages: Action
		): ActionSequence {
			return ActionSequence(id, arrayOf(*stages))
		}
	}

	fun getAction(index: Int): Action? = stages.getOrNull(index)

	fun isEnd(index: Int): Boolean {
		return index >= stages.size - 1
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ActionSequence) return false

		if (id != other.id) return false
		if (!stages.contentEquals(other.stages)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + stages.contentHashCode()
		return result
	}
}