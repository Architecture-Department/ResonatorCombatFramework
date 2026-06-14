package architecture.resonator_combat_framework.module.entity_animation.animation.molang.value

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang AST 节点: 括号分组 (expr)

class Group(private val value: MolangValue) : MolangValue {
	override fun eval(context: MolangData?): Double {
		return value.eval(context)
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}

	override fun toString(): String {
		return "($value)"
	}
}

