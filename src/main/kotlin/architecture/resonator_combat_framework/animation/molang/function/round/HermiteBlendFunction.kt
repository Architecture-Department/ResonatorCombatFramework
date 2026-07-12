package architecture.resonator_combat_framework.animation.molang.function.round

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

// MoLang 函数: math.hermite_blend(a) — Hermite 平滑插值
import architecture.resonator_combat_framework.animation.molang.function.MolangFunction

class HermiteBlendFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		val v = value.eval(context)
		return v * v * (3 - 2 * v)
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

