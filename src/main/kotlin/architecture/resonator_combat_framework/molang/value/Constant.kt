package architecture.resonator_combat_framework.molang.value

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

data class Constant(val value: Double) : MolangValue {
	override fun eval(context: MolangDataHolder?): Double = value
	override fun toString(): String = value.toString()
}
