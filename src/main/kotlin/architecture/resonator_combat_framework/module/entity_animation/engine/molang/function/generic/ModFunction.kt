// MoLang 函数: math.mod(a, b) — 取模
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction

class ModFunction(private val a: MolangValue, private val b: MolangValue) : MolangFunction {
	override fun get(): Double {
		return a.get() % b.get()
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable()
	}
}

