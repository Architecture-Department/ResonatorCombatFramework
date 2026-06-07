package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang AST 节点: 三元运算 condition ? true : false
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

@JvmRecord
data class Ternary(val condition: MolangValue, val trueValue: MolangValue, val falseValue: MolangValue) : MolangValue {
	override fun get(context: MolangData?): Double {
		return if (condition.get(context) != 0.0) trueValue.get(context) else falseValue.get(context)
	}

	override fun isMutable(): Boolean {
		return condition.isMutable() || trueValue.isMutable() || falseValue.isMutable()
	}

	override fun toString(): String {
		return "$condition ? $trueValue : $falseValue"
	}
}

