// MoLang 函数: math.die_roll_integer(sides, rolls) — 骰子投掷（整数和）
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.random

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MathFunction

class DieRollIntegerFunction(private val sides: MathValue, private val rolls: MathValue) : MathFunction {
	override fun get(): Double {
		val s = sides.get()
		val r = rolls.get()
		var sum = 0.0
		for (i in 0..<r.toInt()) sum += (1 + (Math.random() * s).toInt()).toDouble()
		return sum.toInt().toDouble()
	}

	override fun isMutable(): Boolean {
		return true
	}
}

