// MoLang 函数: math.clamp(v, min, max) — 钳制
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.limit

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.max
import kotlin.math.min

class ClampFunction(private val value: MolangValue, private val min: MolangValue, private val max: MolangValue) :
	MolangFunction {
	override fun get(): Double {
		return max(min.get(), min(max.get(), value.get()))
	}

	override fun isMutable(): Boolean {
		return value.isMutable() || min.isMutable() || max.isMutable()
	}
}

