// MoLang 函数: math.to_deg(a) — 弧度转角度
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.misc

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MathFunction

class ToDegFunction(private val value: MathValue) : MathFunction {
	override fun get(): Double {
		return Math.toDegrees(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

