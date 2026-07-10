package architecture.resonator_combat_framework.module.animation.molang.function.generic

import architecture.resonator_combat_framework.module.animation.molang.MolangData
import architecture.resonator_combat_framework.module.animation.molang.MolangValue

/**
 * MoLang 内置函数 —— math.sin(angle)，返回指定角度的正弦值。
 */
import architecture.resonator_combat_framework.module.animation.molang.function.MolangFunction
import kotlin.math.sin

class SinFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return sin(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

