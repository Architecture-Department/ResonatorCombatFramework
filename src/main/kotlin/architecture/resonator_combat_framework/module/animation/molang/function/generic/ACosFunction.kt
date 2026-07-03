package architecture.resonator_combat_framework.module.animation.molang.function.generic

import architecture.resonator_combat_framework.module.animation.molang.MolangData
import architecture.resonator_combat_framework.module.animation.molang.MolangValue
import architecture.resonator_combat_framework.module.animation.molang.function.MolangFunction
import kotlin.math.acos

// MoLang 函数: math.acos(a) — 反余弦
class ACosFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return acos(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

