// MoLang 函数: math.ceil(a) — 向上取整
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.round

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction
import kotlin.math.ceil

class CeilFunction(private val value: MathValue) : MathFunction {
	override fun get(): Double {
		return ceil(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}



