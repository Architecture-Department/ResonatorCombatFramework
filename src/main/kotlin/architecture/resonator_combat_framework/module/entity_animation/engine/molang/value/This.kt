package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

object This : MolangValue {
	override fun get(context: MolangData?): Double = context?.thisValue?.asDouble ?: 0.0
	override fun isMutable(): Boolean = false
	override fun toString(): String = "this"
}
