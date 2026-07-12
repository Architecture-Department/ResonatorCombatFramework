package architecture.resonator_combat_framework.animation.molang.function.generic

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

// MoLang 函数: math.pow(a, b) — 幂
import architecture.resonator_combat_framework.animation.molang.function.MolangFunction
import kotlin.math.pow

class PowFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return a.eval(context).pow(b.eval(context))
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}

