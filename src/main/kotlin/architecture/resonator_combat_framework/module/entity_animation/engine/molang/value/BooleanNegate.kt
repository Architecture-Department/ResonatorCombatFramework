package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang AST 节点: 逻辑非 !expr
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

class BooleanNegate(private val value: MolangValue) : MolangValue {
	override fun get(context: MolangData?): Double {
		return (if (value.get(context) == 0.0) 1 else 0).toDouble()
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}

	override fun toString(): String {
		return "!$value"
	}
}

