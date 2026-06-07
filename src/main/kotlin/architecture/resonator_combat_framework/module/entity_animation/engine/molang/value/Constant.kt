package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

data class Constant(val value: Double) : MolangValue {
	override fun get(context: MolangData?): Double = value
	override fun toString(): String = value.toString()
}
