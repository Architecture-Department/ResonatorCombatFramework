// MoLang 函数: math.clamp(v, min, max) — 钳制
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.limit

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MathFunction
import kotlin.math.max
import kotlin.math.min

class ClampFunction(private val value: MathValue, private val min: MathValue, private val max: MathValue) :
	MathFunction {
	override fun get(): Double {
		return max(min.get(), min(max.get(), value.get()))
	}

	override fun isMutable(): Boolean {
		return value.isMutable() || min.isMutable() || max.isMutable()
	}
}

