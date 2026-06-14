package architecture.resonator_combat_framework.module.entity_animation.animation.molang.value

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

class VariableAssignment(
	private val variable: Variable,
	private val value: MolangValue
) : MolangValue {
	override fun eval(context: MolangData?): Double {
		val result = value.eval(context)
		if (context != null) context.assign(variable.name, result)
		return result
	}

	override fun toString(): String = "${variable.name} = $value"
}
