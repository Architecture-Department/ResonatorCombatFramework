package architecture.resonator_combat_framework.animation.molang.function.random

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

// MoLang 函数: math.random(a, b) — [a,b) 随机浮点数
import architecture.resonator_combat_framework.animation.molang.function.MolangFunction

class RandomFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return a.eval(context) + Math.random() * (b.eval(context) - a.eval(context))
	}

	override fun isMutable(): Boolean {
		return true
	}
}

