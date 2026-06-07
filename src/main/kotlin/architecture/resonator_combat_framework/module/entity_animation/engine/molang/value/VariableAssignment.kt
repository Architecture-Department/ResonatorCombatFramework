package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

class VariableAssignment(
	private val variable: Variable,
	private val value: MolangValue
) : MolangValue {
	override fun get(context: MolangData?): Double {
		val result = value.get(context)
		if (context != null) context.assign(variable.name, result)
		return result
	}

	override fun toString(): String = "${variable.name} = $value"
}
