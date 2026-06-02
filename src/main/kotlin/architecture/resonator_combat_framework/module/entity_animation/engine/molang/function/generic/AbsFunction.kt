// MoLang 函数: math.abs(a) — 绝对值
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MathFunction
import kotlin.math.abs

class AbsFunction(private val value: MathValue) : MathFunction {
	override fun get(): Double {
		return abs(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

