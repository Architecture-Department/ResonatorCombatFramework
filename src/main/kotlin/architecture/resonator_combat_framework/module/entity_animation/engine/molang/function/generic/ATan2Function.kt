// MoLang 函数: math.atan2(y, x) — 双参数反正切
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction
import kotlin.math.atan2

class ATan2Function(private val y: MolangValue, private val x: MolangValue) : MolangFunction {
	override fun get(): Double {
		return atan2(y.get(), x.get())
	}

	override fun isMutable(): Boolean {
		return y.isMutable() || x.isMutable()
	}
}

