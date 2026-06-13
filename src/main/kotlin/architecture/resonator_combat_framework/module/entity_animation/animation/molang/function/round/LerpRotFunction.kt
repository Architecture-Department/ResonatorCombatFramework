package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.round

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.lerprotate(a, b, t) — 角度线性插值
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction

class LerpRotFunction(private val a: MolangValue, private val b: MolangValue, private val t: MolangValue) :
	MolangFunction {
	override fun get(context: MolangData?): Double {
		val va = a.get(context)
		val vb = b.get(context)
		val vt = t.get(context)
		val diff = ((vb - va) % 360 + 540) % 360 - 180
		return va + diff * vt
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable() || t.isMutable()
	}
}

