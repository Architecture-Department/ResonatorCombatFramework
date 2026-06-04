// MoLang 函数: math.sin(a) — 正弦
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.sin

class SinFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return sin(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

