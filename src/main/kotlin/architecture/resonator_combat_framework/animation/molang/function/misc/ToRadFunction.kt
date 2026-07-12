package architecture.resonator_combat_framework.animation.molang.function.misc

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

// MoLang 函数: math.to_rad(a) — 角度转弧度
import architecture.resonator_combat_framework.animation.molang.function.MolangFunction

class ToRadFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return Math.toRadians(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

