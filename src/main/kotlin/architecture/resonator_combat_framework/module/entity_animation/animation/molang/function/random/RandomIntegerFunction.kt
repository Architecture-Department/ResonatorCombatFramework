package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.random

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.random_integer(a, b) — [a,b] 随机整数
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction
import kotlin.math.floor

class RandomIntegerFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return floor(a.eval(context) + Math.random() * (b.eval(context) - a.eval(context) + 1))
	}

	override fun isMutable(): Boolean {
		return true
	}
}

