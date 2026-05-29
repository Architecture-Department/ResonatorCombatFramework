// MoLang 函数: math.atan2(y, x) — 双参数反正切
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.generic

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction
import kotlin.math.atan2

class ATan2Function(private val y: MathValue, private val x: MathValue) : MathFunction {
	override fun get(): Double {
		return atan2(y.get(), x.get())
	}

	override fun isMutable(): Boolean {
		return y.isMutable() || x.isMutable()
	}
}



