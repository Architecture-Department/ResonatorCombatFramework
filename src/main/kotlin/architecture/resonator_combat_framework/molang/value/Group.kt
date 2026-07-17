package architecture.resonator_combat_framework.molang.value

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang AST 节点: 括号分组 (expr)

class Group(private val value: MolangValue) : MolangValue {
	override fun eval(context: MolangDataHolder?): Double {
		return value.eval(context)
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}

	override fun toString(): String {
		return "($value)"
	}
}

