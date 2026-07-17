package architecture.resonator_combat_framework.molang.function.round

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang 函数: math.floor(a) — 向下取整
import architecture.resonator_combat_framework.molang.function.MolangFunction
import kotlin.math.floor

class FloorFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangDataHolder?): Double {
		return floor(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

