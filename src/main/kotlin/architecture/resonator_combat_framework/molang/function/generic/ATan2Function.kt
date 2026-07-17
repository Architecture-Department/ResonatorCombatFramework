package architecture.resonator_combat_framework.molang.function.generic

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.atan2(y, x) — 双参数反正切
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.atan2

class ATan2Function(private val y: MolangValue, private val x: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return atan2(y.eval(context), x.eval(context))
	}

	override fun isMutable(): Boolean {
		return y.isMutable() || x.isMutable()
	}
}

