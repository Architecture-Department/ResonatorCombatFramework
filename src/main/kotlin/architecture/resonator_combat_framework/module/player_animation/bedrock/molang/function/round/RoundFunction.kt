// MoLang 函数: math.round(a) — 四舍五入
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.round

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction

class RoundFunction(private val value: MathValue) : MathFunction {
	override fun get(): Double {
		return Math.round(value.get()).toDouble()
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}



