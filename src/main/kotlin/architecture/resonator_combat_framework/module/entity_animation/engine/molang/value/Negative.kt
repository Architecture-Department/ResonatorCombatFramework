// MoLang AST 节点: 一元负号 -expr
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue

class Negative(private val value: MathValue) : MathValue {
	override fun get(): Double {
		return -value.get()
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}

	override fun toString(): String {
		return "-" + value
	}
}

