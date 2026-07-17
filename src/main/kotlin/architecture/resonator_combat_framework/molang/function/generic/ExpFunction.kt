package architecture.resonator_combat_framework.molang.function.generic

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.exp(a) — 指数
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.exp

class ExpFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return exp(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

