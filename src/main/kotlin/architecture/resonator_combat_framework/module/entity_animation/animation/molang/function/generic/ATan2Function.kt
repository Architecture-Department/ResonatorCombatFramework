package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.atan2(y, x) — 双参数反正切
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction
import kotlin.math.atan2

class ATan2Function(private val y: MolangValue, private val x: MolangValue) : MolangFunction {
	override fun get(context: MolangData?): Double {
		return atan2(y.get(context), x.get(context))
	}

	override fun isMutable(): Boolean {
		return y.isMutable() || x.isMutable()
	}
}

