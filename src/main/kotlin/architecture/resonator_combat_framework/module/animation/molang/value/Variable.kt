package architecture.resonator_combat_framework.module.animation.molang.value

import architecture.resonator_combat_framework.module.animation.molang.MolangData
import architecture.resonator_combat_framework.module.animation.molang.MolangValue

/**
 * MoLang AST 节点 —— 变量引用（如 v.xxx, q.xxx, temp.xxx 等）。
 * 从 [MolangData] 的变量作用域中按名称查找值。
 */
class Variable(val name: String) : MolangValue {
	override fun eval(context: MolangData?): Double = context?.resolve(name) ?: 0.0
	override fun toString(): String = name
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Variable) return false
		return name == other.name
	}

	override fun hashCode(): Int = name.hashCode()
}
