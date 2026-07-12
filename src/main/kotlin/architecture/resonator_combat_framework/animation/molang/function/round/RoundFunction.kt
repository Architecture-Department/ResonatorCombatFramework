package architecture.resonator_combat_framework.animation.molang.function.round

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

// MoLang 函数: math.round(a) — 四舍五入
import architecture.resonator_combat_framework.animation.molang.function.MolangFunction

class RoundFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return Math.round(value.eval(context)).toDouble()
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

