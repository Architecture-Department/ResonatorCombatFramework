package architecture.resonator_combat_framework.molang.function.random

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.random_integer(a, b) — [a,b] 随机整数
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.floor

class RandomIntegerFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return floor(a.eval(context) + Math.random() * (b.eval(context) - a.eval(context) + 1))
	}

	override fun isMutable(): Boolean {
		return true
	}
}

