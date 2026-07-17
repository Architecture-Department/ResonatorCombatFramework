package architecture.resonator_combat_framework.molang.value

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

class VariableAssignment(
	private val variable: Variable,
	private val value: MolangValue
) : MolangValue {
	override fun eval(context: MolangDataHolder?): Double {
		val result = value.eval(context)
		if (context != null) context.assign(variable.name, result)
		return result
	}

	override fun toString(): String = "${variable.name} = $value"
}
