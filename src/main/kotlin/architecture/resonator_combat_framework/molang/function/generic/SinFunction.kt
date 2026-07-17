package architecture.resonator_combat_framework.molang.function.generic

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

/**
 * MoLang 内置函数 —— math.sin(angle)，返回指定角度的正弦值。
 */
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.sin

class SinFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return sin(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

