// MoLang 函数: math.to_rad(a) — 角度转弧度
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.misc

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction

class ToRadFunction(private val value: MathValue) : MathFunction {
	override fun get(): Double {
		return Math.toRadians(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}



