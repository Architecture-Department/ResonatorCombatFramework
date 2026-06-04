// MoLang AST 节点: 常量数值
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

@JvmRecord
data class Constant(val value: Double) : MolangValue {
	override fun get(): Double {
		return value
	}

	override fun isMutable(): Boolean {
		return false
	}

	override fun toString(): String {
		return value.toString()
	}
}

