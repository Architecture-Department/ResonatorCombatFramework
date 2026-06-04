// MoLang AST 节点: 三元运算 condition ? true : false
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

@JvmRecord
data class Ternary(val condition: MolangValue, val trueValue: MolangValue, val falseValue: MolangValue) : MolangValue {
	override fun get(): Double {
		return if (condition.get() != 0.0) trueValue.get() else falseValue.get()
	}

	override fun isMutable(): Boolean {
		return condition.isMutable() || trueValue.isMutable() || falseValue.isMutable()
	}

	override fun toString(): String {
		return "$condition ? $trueValue : $falseValue"
	}
}

