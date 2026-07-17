package architecture.resonator_combat_framework.molang.function.generic

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.abs(a) — 绝对值
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.abs

class AbsFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return abs(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

