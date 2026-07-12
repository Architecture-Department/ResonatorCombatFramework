package architecture.resonator_combat_framework.animation.molang.value

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

data class Constant(val value: Double) : MolangValue {
	override fun eval(context: MolangData?): Double = value
	override fun toString(): String = value.toString()
}
