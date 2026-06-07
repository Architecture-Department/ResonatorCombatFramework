package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.limit

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang 函数: math.min(a, b) — 最小值
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.min

class MinFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun get(context: MolangData?): Double {
		return min(a.get(context), b.get(context))
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}

