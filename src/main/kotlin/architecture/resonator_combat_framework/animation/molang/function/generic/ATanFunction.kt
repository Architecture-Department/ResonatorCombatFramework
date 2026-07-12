package architecture.resonator_combat_framework.animation.molang.function.generic

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

// MoLang 函数: math.atan(a) — 反正切
import architecture.resonator_combat_framework.animation.molang.function.MolangFunction
import kotlin.math.atan

class ATanFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return atan(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

