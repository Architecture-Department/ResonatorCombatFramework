// MoLang AST 节点: 常量数值
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.value

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue

@JvmRecord
data class Constant(val value: Double) : MathValue {
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



