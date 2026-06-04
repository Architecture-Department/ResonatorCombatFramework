// MoLang 函数: math.acos(a) — 反余弦
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.acos

class ACosFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return acos(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

