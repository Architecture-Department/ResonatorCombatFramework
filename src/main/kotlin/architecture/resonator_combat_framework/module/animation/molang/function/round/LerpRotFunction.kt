package architecture.resonator_combat_framework.module.animation.molang.function.round

import architecture.resonator_combat_framework.module.animation.molang.MolangData
import architecture.resonator_combat_framework.module.animation.molang.MolangValue

// MoLang 函数: math.lerprotate(a, b, t) — 角度线性插值
import architecture.resonator_combat_framework.module.animation.molang.function.MolangFunction

class LerpRotFunction(private val a: MolangValue, private val b: MolangValue, private val t: MolangValue) :
	MolangFunction {
	override fun eval(context: MolangData?): Double {
		val va = a.eval(context)
		val vb = b.eval(context)
		val vt = t.eval(context)
		val diff = ((vb - va) % 360 + 540) % 360 - 180
		return va + diff * vt
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable() || t.isMutable()
	}
}

