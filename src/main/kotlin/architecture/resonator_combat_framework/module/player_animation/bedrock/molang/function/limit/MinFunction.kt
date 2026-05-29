// MoLang 函数: math.min(a, b) — 最小值
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.limit

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction
import kotlin.math.min

class MinFunction(private val a: MathValue, private val b: MathValue) : MathFunction {
	override fun get(): Double {
		return min(a.get(), b.get())
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}



