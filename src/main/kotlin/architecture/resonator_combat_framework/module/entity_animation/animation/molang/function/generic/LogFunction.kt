package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.log(a) — 自然对数（mojang 标准）
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction
import kotlin.math.ln

class LogFunction(private val value: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		return ln(value.eval(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}
