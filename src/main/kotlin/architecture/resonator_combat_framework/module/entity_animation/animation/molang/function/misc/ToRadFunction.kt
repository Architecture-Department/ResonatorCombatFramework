package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.misc

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.to_rad(a) — 角度转弧度
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction

class ToRadFunction(private val value: MolangValue) : MolangFunction {
	override fun get(context: MolangData?): Double {
		return Math.toRadians(value.get(context))
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

