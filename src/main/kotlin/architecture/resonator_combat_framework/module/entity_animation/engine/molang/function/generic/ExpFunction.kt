// MoLang 函数: math.exp(a) — 指数
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.exp

class ExpFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return exp(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

