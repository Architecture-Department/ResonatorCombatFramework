// MoLang 函数: math.trunc(a) — 截断取整
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.round

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.ceil
import kotlin.math.floor

class TruncateFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return if (value.get() < 0) ceil(value.get()) else floor(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

