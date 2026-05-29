// MoLang 函数: math.mod(a, b) — 取模
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.generic

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction

class ModFunction(private val a: MathValue, private val b: MathValue) : MathFunction {
	override fun get(): Double {
		return a.get() % b.get()
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}



