package architecture.resonator_combat_framework.combat

import architecture.goldenboughs_lib.api.AllOpen
import java.util.function.Supplier

/**
 * 动作序列 —— 由多个 [Action] 组成的连击序列。
 * 通过 [ActionController] 驱动，按顺序依次播放各段动作，支持循环和打断。
 * 属于包装类
 *
 * @param stages 动作数组，按连击段顺序排列
 */
@AllOpen
data class ActionSequence(
	val stages: Array<Supplier<out Action>>,
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
			vararg stages: Supplier<out Action>
		): ActionSequence {
			return ActionSequence(arrayOf(*stages))
		}
	}

	/**
	 * 获取指定索引的动作。
	 *
	 * @param index 动作在序列中的索引
	 * @return 对应动作，索引越界时返回 null
	 */
	fun getAction(index: Int): Action? = stages.getOrNull(index)?.get()

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

		stages.forEach {
			if (it.get() != other.stages) {
				return false
			}
		}

		return true
	}

	override fun hashCode(): Int {
		return stages.sumOf { it.get().hashCode() }
	}
}
