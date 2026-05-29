// MoLang 函数: math.random(a, b) — [a,b) 随机浮点数
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.random

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction

class RandomFunction(private val a: MathValue, private val b: MathValue) : MathFunction {
	override fun get(): Double {
		return a.get() + Math.random() * (b.get() - a.get())
	}

	override fun isMutable(): Boolean {
		return true
	}
}



