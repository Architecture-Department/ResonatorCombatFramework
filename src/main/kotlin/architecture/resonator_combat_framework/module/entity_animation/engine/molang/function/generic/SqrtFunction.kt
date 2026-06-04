// MoLang 函数: math.sqrt(a) — 平方根
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.sqrt

class SqrtFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return sqrt(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

