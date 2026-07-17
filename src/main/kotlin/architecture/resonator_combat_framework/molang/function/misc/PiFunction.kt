package architecture.resonator_combat_framework.molang.function.misc

import architecture.resonator_combat_framework.molang.MolangDataHolder

// MoLang 函数: math.pi() — 圆周率常数
import architecture.resonator_combat_framework.molang.function.MolangFunction

class PiFunction : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return Math.PI
	}

	override fun isMutable(): Boolean {
		return false
	}
}

