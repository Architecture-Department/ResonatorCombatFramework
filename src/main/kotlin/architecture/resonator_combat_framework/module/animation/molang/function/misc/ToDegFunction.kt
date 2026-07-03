package architecture.resonator_combat_framework.module.animation.molang.function.misc

import architecture.resonator_combat_framework.module.animation.molang.MolangData
import architecture.resonator_combat_framework.module.animation.molang.MolangValue


import architecture.resonator_combat_framework.module.animation.molang.function.MolangFunction

// MoLang 函数: math.to_deg(a) — 弧度转角度
class ToDegFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return Math.toDegrees(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

