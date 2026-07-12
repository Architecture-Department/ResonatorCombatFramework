package architecture.resonator_combat_framework.animation.molang.function.generic

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

// MoLang 函数: math.sqrt(a) — 平方根
import architecture.resonator_combat_framework.animation.molang.function.MolangFunction
import kotlin.math.sqrt

class SqrtFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return sqrt(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

