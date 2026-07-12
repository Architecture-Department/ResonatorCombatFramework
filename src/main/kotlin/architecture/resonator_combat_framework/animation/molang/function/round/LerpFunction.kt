package architecture.resonator_combat_framework.animation.molang.function.round

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

// MoLang 函数: math.lerp(a, b, t) — 线性插值
import architecture.resonator_combat_framework.animation.molang.function.MolangFunction

class LerpFunction(private val a: MolangValue, private val b: MolangValue, private val t: MolangValue) :
	MolangFunction {
	override fun eval(context: MolangData?): Double {
		return a.eval(context) + (b.eval(context) - a.eval(context)) * t.eval(context)
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable() || t.isMutable()
	}
}

