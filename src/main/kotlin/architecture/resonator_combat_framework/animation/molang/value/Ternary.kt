package architecture.resonator_combat_framework.animation.molang.value

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

/**
 * MoLang AST 节点 —— 三元条件运算（condition ? true_value : false_value）。
 * condition 非 0 时取 trueValue，否则取 falseValue。
 */
@JvmRecord
data class Ternary(val condition: MolangValue, val trueValue: MolangValue, val falseValue: MolangValue) : MolangValue {
	override fun eval(context: MolangData?): Double {
		return if (condition.eval(context) != 0.0) trueValue.eval(context) else falseValue.eval(context)
	}

	override fun isMutable(): Boolean {
		return condition.isMutable() || trueValue.isMutable() || falseValue.isMutable()
	}

	override fun toString(): String {
		return "$condition ? $trueValue : $falseValue"
	}
}

