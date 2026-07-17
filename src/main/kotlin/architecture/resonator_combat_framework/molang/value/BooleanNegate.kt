package architecture.resonator_combat_framework.molang.value

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang AST 节点: 逻辑非 !expr

class BooleanNegate(private val value: MolangValue) : MolangValue {
	override fun eval(context: MolangDataHolder?): Double {
		return (if (value.eval(context) == 0.0) 1 else 0).toDouble()
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}

	override fun toString(): String {
		return "!$value"
	}
}

