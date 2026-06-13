package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.round

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.floor(a) — 向下取整
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction
import kotlin.math.floor

class FloorFunction(private val value: MolangValue) : MolangFunction {
	override fun get(context: MolangData?): Double {
		return floor(value.get(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

