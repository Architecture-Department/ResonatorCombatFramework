package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.random

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang 函数: math.random_integer(a, b) — [a,b] 随机整数
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.floor

class RandomIntegerFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun get(context: MolangData?): Double {
		return floor(a.get(context) + Math.random() * (b.get(context) - a.get(context) + 1))
	}

	override fun isMutable(): Boolean {
		return true
	}
}

