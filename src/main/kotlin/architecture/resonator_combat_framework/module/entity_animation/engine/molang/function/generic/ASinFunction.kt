// MoLang 函数: math.asin(a) — 反正弦
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.asin

class ASinFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return asin(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

