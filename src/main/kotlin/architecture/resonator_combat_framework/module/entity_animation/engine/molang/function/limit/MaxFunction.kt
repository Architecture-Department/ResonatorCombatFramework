// MoLang 函数: math.max(a, b) — 最大值
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.limit

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.max

class MaxFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun get(): Double {
		return max(a.get(), b.get())
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}

