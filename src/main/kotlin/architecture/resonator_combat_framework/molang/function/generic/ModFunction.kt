package architecture.resonator_combat_framework.molang.function.generic

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.mod(a, b) — 取模
import architecture.resonator_combat_framework.molang.function.MolangFunction

class ModFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return a.eval(context) % b.eval(context)
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}

