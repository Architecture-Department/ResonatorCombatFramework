// MoLang 函数: math.die_roll(sides, rolls) — 骰子投掷（浮点和）
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.random

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction

class DieRollFunction(private val sides: MathValue, private val rolls: MathValue) : MathFunction {
	override fun get(): Double {
		val s = sides.get()
		val r = rolls.get()
		var sum = 0.0
		for (i in 0..<r.toInt()) sum += (1 + (Math.random() * s).toInt()).toDouble()
		return sum
	}

	override fun isMutable(): Boolean {
		return true
	}
}



