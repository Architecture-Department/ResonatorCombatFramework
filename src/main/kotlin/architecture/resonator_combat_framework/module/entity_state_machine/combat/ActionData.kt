package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.goldenboughs_lib.api.AllOpe
import net.minecraft.resources.ResourceLocation

/**
 * 战斗动作数据 —— 定义一个完整的多段攻击序列。
 */
@AllOpe
data class ActionData(
	val id: ResourceLocation,
	val stages: Array<Action>,
) {
	fun getStage(index: Int): Action? = stages.getOrNull(index)

	fun isEnd(index: Int): Boolean {
		return index >= stages.size - 1
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ActionData) return false

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