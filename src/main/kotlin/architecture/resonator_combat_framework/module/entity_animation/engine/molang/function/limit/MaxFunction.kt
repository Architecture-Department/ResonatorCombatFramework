// MoLang 函数: math.max(a, b) — 最大值
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.limit

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MathFunction
import kotlin.math.max

class MaxFunction(private val a: MathValue, private val b: MathValue) : MathFunction {
	override fun get(): Double {
		return max(a.get(), b.get())
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}

