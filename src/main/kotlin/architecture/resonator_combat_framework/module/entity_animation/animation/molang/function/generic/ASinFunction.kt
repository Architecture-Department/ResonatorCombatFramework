package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.asin(a) — 反正弦
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction
import kotlin.math.asin

class ASinFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return asin(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

