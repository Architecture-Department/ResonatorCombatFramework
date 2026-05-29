// MoLang 函数: math.exp(a) — 指数
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.generic

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction
import kotlin.math.exp

class ExpFunction(private val value: MathValue) : MathFunction {
	override fun get(): Double {
		return exp(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}



