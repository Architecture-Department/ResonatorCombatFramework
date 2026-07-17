package architecture.resonator_combat_framework.molang.function.generic

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.asin(a) — 反正弦
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.asin

class ASinFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return asin(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

