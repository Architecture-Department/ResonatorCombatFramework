package architecture.resonator_combat_framework.molang.function.round

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.trunc(a) — 截断取整
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.ceil
import kotlin.math.floor

class TruncateFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return if (value.eval(context) < 0) ceil(value.eval(context)) else floor(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

