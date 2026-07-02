package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.goldenboughs_lib.api.AllOpe
import net.minecraft.resources.ResourceLocation

/**
 * 动作序列
 */
/**
 * 动作序列 —— 由多个 [Action] 组成的连击序列。
 * 通过 [ActionController] 驱动，按顺序依次播放各段动作，支持循环和打断。
 *
 * @param id 序列的唯一标识符
 * @param stages 动作数组，按连击段顺序排列
 */
@AllOpe
data class ActionSequence(
	val id: ResourceLocation,
	val stages: Array<Action>,
) {
	companion object {
		/**
		 * 快捷创建动作序列。
		 *
		 * @param id 序列标识符
		 * @param stages 按顺序排列的动作
		 * @return 新动作序列
		 */
		@JvmStatic
		fun of(
			id: ResourceLocation,
			vararg stages: Action
		): ActionSequence {
			return ActionSequence(id, arrayOf(*stages))
		}
	}

	/**
	 * 获取指定索引的动作。
	 *
	 * @param index 动作在序列中的索引
	 * @return 对应动作，索引越界时返回 null
	 */
	fun getAction(index: Int): Action? = stages.getOrNull(index)

	/**
	 * 判断指定索引是否为序列末段。
	 *
	 * @param index 要检查的索引
	 * @return 是否为末段
	 */
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
