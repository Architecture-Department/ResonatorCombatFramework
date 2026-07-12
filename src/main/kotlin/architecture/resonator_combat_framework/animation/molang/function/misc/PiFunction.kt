package architecture.resonator_combat_framework.animation.molang.function.misc

import architecture.resonator_combat_framework.animation.molang.MolangData

// MoLang 函数: math.pi() — 圆周率常数
import architecture.resonator_combat_framework.animation.molang.function.MolangFunction

class PiFunction : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return Math.PI
	}

	override fun isMutable(): Boolean {
		return false
	}
}

