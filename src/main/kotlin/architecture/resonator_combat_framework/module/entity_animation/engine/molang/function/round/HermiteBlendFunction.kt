// MoLang 函数: math.hermite_blend(a) — Hermite 平滑插值
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.round

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction

class HermiteBlendFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		val v = value.get()
		return v * v * (3 - 2 * v)
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

