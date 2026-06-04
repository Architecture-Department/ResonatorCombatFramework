// MoLang 函数: math.pow(a, b) — 幂
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.pow

class PowFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun get(): Double {
		return a.get().pow(b.get())
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}

