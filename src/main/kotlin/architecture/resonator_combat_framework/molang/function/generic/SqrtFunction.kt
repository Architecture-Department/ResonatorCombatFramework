package architecture.resonator_combat_framework.molang.function.generic

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.sqrt(a) — 平方根
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.sqrt

class SqrtFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return sqrt(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

