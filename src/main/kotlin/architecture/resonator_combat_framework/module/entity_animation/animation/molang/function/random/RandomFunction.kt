package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.random

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.random(a, b) — [a,b) 随机浮点数
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction

class RandomFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun get(context: MolangData?): Double {
		return a.get(context) + Math.random() * (b.get(context) - a.get(context))
	}

	override fun isMutable(): Boolean {
		return true
	}
}

