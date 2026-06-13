package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.ln(a) / math.log(a) — 自然对数
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction
import kotlin.math.ln

class LogFunction(private val value: MolangValue) : MolangFunction {
	override fun get(context: MolangData?): Double {
		return ln(value.get(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

