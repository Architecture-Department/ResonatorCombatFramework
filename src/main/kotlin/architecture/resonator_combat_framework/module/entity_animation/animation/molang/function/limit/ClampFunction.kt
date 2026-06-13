package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.limit

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.clamp(v, min, max) — 钳制
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction
import kotlin.math.max
import kotlin.math.min

class ClampFunction(private val value: MolangValue, private val min: MolangValue, private val max: MolangValue) :
	MolangFunction {
	override fun get(context: MolangData?): Double {
		return max(min.get(context), min(max.get(context), value.get(context)))
	}

	override fun isMutable(): Boolean {
		return value.isMutable() || min.isMutable() || max.isMutable()
	}
}

