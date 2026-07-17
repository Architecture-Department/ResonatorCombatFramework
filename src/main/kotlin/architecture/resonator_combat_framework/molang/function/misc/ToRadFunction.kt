package architecture.resonator_combat_framework.molang.function.misc

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.to_rad(a) — 角度转弧度
import architecture.resonator_combat_framework.molang.function.MolangFunction

class ToRadFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return Math.toRadians(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

