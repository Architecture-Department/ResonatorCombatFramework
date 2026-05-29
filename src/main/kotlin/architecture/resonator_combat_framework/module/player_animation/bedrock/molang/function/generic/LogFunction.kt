// MoLang 函数: math.ln(a) / math.log(a) — 自然对数
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.generic

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction
import kotlin.math.ln

class LogFunction(private val value: MathValue) : MathFunction {
	override fun get(): Double {
		return ln(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}



