package architecture.resonator_combat_framework.molang.function.generic

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.atan(a) — 反正切
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.atan

class ATanFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return atan(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

