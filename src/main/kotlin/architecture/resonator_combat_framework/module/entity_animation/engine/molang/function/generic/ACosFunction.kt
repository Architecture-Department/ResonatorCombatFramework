// MoLang 函数: math.acos(a) — 反余弦
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MathFunction
import kotlin.math.acos

class ACosFunction(private val value: MathValue) : MathFunction {
	override fun get(): Double {
		return acos(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

