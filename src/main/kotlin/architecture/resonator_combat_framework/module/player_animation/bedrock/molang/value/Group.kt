// MoLang AST 节点: 括号分组 (expr)
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.value

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue

class Group(private val value: MathValue) : MathValue {
	override fun get(): Double {
		return value.get()
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}

	override fun toString(): String {
		return "($value)"
	}
}



