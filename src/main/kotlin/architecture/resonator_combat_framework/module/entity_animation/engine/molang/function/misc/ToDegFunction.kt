// MoLang 函数: math.to_deg(a) — 弧度转角度
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.misc

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction

class ToDegFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return Math.toDegrees(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

