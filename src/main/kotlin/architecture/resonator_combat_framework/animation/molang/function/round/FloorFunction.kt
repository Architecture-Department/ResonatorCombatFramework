package architecture.resonator_combat_framework.animation.molang.function.round

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

// MoLang 函数: math.floor(a) — 向下取整
import architecture.resonator_combat_framework.animation.molang.function.MolangFunction
import kotlin.math.floor

class FloorFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return floor(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

