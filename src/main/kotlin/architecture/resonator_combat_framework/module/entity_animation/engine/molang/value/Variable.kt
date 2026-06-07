package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

class Variable(val name: String) : MolangValue {
	override fun get(context: MolangData?): Double = context?.resolve(name) ?: 0.0
	override fun toString(): String = name
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Variable) return false
		return name == other.name
	}

	override fun hashCode(): Int = name.hashCode()
}
