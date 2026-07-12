package architecture.resonator_combat_framework.animation.molang.function.limit

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

// MoLang 函数: math.clamp(v, min, max) — 钳制
import architecture.resonator_combat_framework.animation.molang.function.MolangFunction
import kotlin.math.max
import kotlin.math.min

class ClampFunction(private val value: MolangValue, private val min: MolangValue, private val max: MolangValue) :
	MolangFunction {
	override fun eval(context: MolangData?): Double {
		return max(min.eval(context), min(max.eval(context), value.eval(context)))
	}

	override fun isMutable(): Boolean {
		return value.isMutable() || min.isMutable() || max.isMutable()
	}
}

