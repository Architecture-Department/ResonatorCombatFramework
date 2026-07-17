package architecture.resonator_combat_framework.molang.function.misc

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue


import architecture.resonator_combat_framework.molang.function.MolangFunction

// MoLang 函数: math.to_deg(a) — 弧度转角度
class ToDegFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return Math.toDegrees(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

