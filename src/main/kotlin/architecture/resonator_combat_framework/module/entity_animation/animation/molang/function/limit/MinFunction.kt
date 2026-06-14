package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.limit

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.min(a, b) — 最小值
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction
import kotlin.math.min

class MinFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return min(a.eval(context), b.eval(context))
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}

