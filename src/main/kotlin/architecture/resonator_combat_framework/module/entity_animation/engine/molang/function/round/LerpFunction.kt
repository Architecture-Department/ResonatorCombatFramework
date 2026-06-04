// MoLang 函数: math.lerp(a, b, t) — 线性插值
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.round

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction

class LerpFunction(private val a: MolangValue, private val b: MolangValue, private val t: MolangValue) :
	MolangFunction {
	override fun get(): Double {
		return a.get() + (b.get() - a.get()) * t.get()
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable() || t.isMutable()
	}
}

