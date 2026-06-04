// MoLang 函数: math.ln(a) / math.log(a) — 自然对数
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.ln

class LogFunction(private val value: MolangValue) : MolangFunction {
	override fun get(): Double {
		return ln(value.get())
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}
}

