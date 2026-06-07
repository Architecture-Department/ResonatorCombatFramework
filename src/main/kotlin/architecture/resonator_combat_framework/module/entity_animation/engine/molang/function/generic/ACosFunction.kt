package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.acos

// MoLang 函数: math.acos(a) — 反余弦
class ACosFunction(private val value: MolangValue) : MolangFunction {
	override fun get(context: MolangData?): Double {
		return acos(value.get(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

