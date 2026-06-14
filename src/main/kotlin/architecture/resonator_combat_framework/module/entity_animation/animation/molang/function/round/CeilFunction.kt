package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.round

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.ceil(a) — 向上取整
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction
import kotlin.math.ceil

class CeilFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return ceil(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

