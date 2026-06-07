package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang AST 节点: 括号分组 (expr)
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

class Group(private val value: MolangValue) : MolangValue {
	override fun get(context: MolangData?): Double {
		return value.get(context)
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}

	override fun toString(): String {
		return "($value)"
	}
}

