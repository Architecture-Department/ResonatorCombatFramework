// MoLang 函数: math.pi() — 圆周率常数
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.misc

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction

class PiFunction : MathFunction {
	override fun get(): Double {
		return Math.PI
	}

	override fun isMutable(): Boolean {
		return false
	}
}



