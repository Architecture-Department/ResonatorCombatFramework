package architecture.resonator_combat_framework.molang.function.generic

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.acos

// MoLang 函数: math.acos(a) — 反余弦
class ACosFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return acos(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

