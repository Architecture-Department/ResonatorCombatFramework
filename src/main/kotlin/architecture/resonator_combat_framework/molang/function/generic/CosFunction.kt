package architecture.resonator_combat_framework.molang.function.generic

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.cos(a) — 余弦
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.cos

class CosFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return cos(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

