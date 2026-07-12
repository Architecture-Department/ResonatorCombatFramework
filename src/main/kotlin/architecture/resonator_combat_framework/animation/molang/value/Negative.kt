package architecture.resonator_combat_framework.animation.molang.value

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

// MoLang AST 节点: 一元负号 -expr

class Negative(private val value: MolangValue) : MolangValue {
	override fun eval(context: MolangData?): Double {
		return -value.eval(context)
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}

	override fun toString(): String {
		return "-" + value
	}
}

