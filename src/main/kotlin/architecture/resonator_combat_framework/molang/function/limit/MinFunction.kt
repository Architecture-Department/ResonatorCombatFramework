package architecture.resonator_combat_framework.molang.function.limit

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.min(a, b) — 最小值
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.min

class MinFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return min(a.eval(context), b.eval(context))
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}

