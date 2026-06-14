package architecture.resonator_combat_framework.module.entity_animation.animation.molang.value

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang AST 节点: 逻辑非 !expr

class BooleanNegate(private val value: MolangValue) : MolangValue {
	override fun eval(context: MolangData?): Double {
		return (if (value.eval(context) == 0.0) 1 else 0).toDouble()
	}

	override fun isMutable(): Boolean {
		return value.isMutable()
	}

	override fun toString(): String {
		return "!$value"
	}
}

