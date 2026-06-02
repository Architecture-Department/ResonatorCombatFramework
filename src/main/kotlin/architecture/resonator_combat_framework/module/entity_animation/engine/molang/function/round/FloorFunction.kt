// MoLang 函数: math.floor(a) — 向下取整
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.round

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MathFunction
import kotlin.math.floor

class FloorFunction(private val value: MathValue) : MathFunction {
	override fun get(): Double {
		return floor(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

