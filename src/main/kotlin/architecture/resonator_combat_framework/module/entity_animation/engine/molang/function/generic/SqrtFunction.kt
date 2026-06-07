package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang 函数: math.sqrt(a) — 平方根
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.sqrt

class SqrtFunction(private val value: MolangValue) : MolangFunction {
	override fun get(context: MolangData?): Double {
		return sqrt(value.get(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

