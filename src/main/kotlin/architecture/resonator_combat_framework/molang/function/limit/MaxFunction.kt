package architecture.resonator_combat_framework.molang.function.limit

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.max(a, b) — 最大值
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.max

class MaxFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return max(a.eval(context), b.eval(context))
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}

