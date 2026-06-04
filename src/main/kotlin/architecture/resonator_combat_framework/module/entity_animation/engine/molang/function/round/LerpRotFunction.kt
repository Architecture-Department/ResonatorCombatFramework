// MoLang 函数: math.lerprotate(a, b, t) — 角度线性插值
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.round

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction

class LerpRotFunction(private val a: MolangValue, private val b: MolangValue, private val t: MolangValue) :
	MolangFunction {
	override fun get(): Double {
		val va = a.get()
		val vb = b.get()
		val vt = t.get()
		val diff = ((vb - va) % 360 + 540) % 360 - 180
		return va + diff * vt
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable() || t.isMutable()
	}
}

