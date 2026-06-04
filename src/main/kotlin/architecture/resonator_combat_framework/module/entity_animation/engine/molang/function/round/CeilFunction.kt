// MoLang 函数: math.ceil(a) — 向上取整
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.round

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.ceil

class CeilFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return ceil(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

