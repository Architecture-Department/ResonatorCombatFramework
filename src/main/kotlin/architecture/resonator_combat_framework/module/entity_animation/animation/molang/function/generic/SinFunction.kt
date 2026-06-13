package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.sin(a) — 正弦
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction
import kotlin.math.sin

class SinFunction(private val value: MolangValue) : MolangFunction {
	override fun get(context: MolangData?): Double {
		return sin(value.get(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

