package architecture.resonator_combat_framework.module.entity_animation.animation.molang.value

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

data class Constant(val value: Double) : MolangValue {
	override fun eval(context: MolangData?): Double = value
	override fun toString(): String = value.toString()
}
