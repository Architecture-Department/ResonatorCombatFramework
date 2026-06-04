// MoLang 函数: math.abs(a) — 绝对值
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.abs

class AbsFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return abs(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

