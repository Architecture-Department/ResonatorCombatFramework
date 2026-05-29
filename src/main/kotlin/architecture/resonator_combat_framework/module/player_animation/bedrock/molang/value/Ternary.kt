// MoLang AST 节点: 三元运算 condition ? true : false
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.value

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue

@JvmRecord
data class Ternary(val condition: MathValue, val trueValue: MathValue, val falseValue: MathValue) : MathValue {
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






