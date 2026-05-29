// MoLang 函数: math.random_integer(a, b) — [a,b] 随机整数
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.random

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction
import kotlin.math.floor

class RandomIntegerFunction(private val a: MathValue, private val b: MathValue) : MathFunction {
	override fun get(): Double {
		return floor(a.get() + Math.random() * (b.get() - a.get() + 1))
	}

	override fun isMutable(): Boolean {
		return true
	}
}



