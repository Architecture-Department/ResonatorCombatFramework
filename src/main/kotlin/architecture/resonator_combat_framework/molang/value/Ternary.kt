package architecture.resonator_combat_framework.molang.value

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

/**
 * MoLang AST 节点 —— 三元条件运算（condition ? true_value : false_value）。
 * condition 非 0 时取 trueValue，否则取 falseValue。
 */
@JvmRecord
data class Ternary(val condition: MolangValue, val trueValue: MolangValue, val falseValue: MolangValue) : MolangValue {
	override fun eval(context: MolangDataHolder?): Double {
		return if (condition.eval(context) != 0.0) trueValue.eval(context) else falseValue.eval(context)
	}

	override fun isMutable(): Boolean {
		return condition.isMutable() || trueValue.isMutable() || falseValue.isMutable()
	}

	override fun toString(): String {
		return "$condition ? $trueValue : $falseValue"
	}
}

