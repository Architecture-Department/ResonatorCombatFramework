// MoLang 函数: math.atan(a) — 反正切
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.generic

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction
import kotlin.math.atan

class ATanFunction(private val value: MathValue) : MathFunction {
	override fun get(): Double {
		return atan(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}



