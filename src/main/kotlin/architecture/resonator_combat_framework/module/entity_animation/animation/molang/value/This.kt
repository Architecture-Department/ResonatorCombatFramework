package architecture.resonator_combat_framework.module.entity_animation.animation.molang.value

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

object This : MolangValue {
	override fun get(context: MolangData?): Double = context?.thisValue?.asDouble ?: 0.0
	override fun isMutable(): Boolean = false
	override fun toString(): String = "this"
}
