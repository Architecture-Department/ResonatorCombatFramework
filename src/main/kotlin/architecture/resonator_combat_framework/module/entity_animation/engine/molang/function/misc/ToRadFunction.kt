// MoLang 函数: math.to_rad(a) — 角度转弧度
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.misc

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction

class ToRadFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return Math.toRadians(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

