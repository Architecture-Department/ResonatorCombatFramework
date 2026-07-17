package architecture.resonator_combat_framework.molang.function.random

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.random(a, b) — [a,b) 随机浮点数
import architecture.resonator_combat_framework.molang.function.MolangFunction

class RandomFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return a.eval(context) + Math.random() * (b.eval(context) - a.eval(context))
	}

	override fun isMutable(): Boolean {
		return true
	}
}

