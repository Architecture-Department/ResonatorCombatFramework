// MoLang 函数: math.atan(a) — 反正切
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.atan

class ATanFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return atan(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

