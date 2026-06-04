// MoLang 函数: math.random(a, b) — [a,b) 随机浮点数
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.random

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction

class RandomFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun get(): Double {
		return a.get() + Math.random() * (b.get() - a.get())
	}

	override fun isMutable(): Boolean {
		return true
	}
}

