// MoLang 函数: math.cos(a) — 余弦
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MathFunction
import kotlin.math.cos

class CosFunction(private val value: MathValue) : MathFunction {
	override fun get(): Double {
		return cos(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

