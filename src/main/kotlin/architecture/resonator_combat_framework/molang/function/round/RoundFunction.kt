package architecture.resonator_combat_framework.molang.function.round

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.round(a) — 四舍五入
import architecture.resonator_combat_framework.molang.function.MolangFunction

class RoundFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return Math.round(value.eval(context)).toDouble()
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

