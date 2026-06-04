// MoLang 函数: math.round(a) — 四舍五入
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.round

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction

class RoundFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return Math.round(value.get()).toDouble()
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

