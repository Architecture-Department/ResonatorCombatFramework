package architecture.resonator_combat_framework.module.animation.molang.function.generic

import architecture.resonator_combat_framework.module.animation.molang.MolangData
import architecture.resonator_combat_framework.module.animation.molang.MolangValue

// MoLang 函数: math.mod(a, b) — 取模
import architecture.resonator_combat_framework.module.animation.molang.function.MolangFunction

class ModFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return a.eval(context) % b.eval(context)
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}

