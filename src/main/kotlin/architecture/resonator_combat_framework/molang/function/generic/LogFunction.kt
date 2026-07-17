package architecture.resonator_combat_framework.molang.function.generic

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.log(a) — 自然对数（mojang 标准）
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.ln

class LogFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return ln(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}
