package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.mod(a, b) — 取模
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction

class ModFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun get(context: MolangData?): Double {
		return a.get(context) % b.get(context)
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}

