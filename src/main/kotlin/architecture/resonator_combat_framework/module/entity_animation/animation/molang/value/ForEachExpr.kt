package architecture.resonator_combat_framework.module.entity_animation.animation.molang.value

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

class ForEachExpr(val variableName: String, val array: MolangValue, val body: MolangValue) : MolangValue {
	override fun get(context: MolangData?): Double {
		val count = array.get(context).toInt().coerceAtLeast(0)
		var result = 0.0
		for (i in 0 until count) {
			val iteratorName = "temp.$variableName"
			context?.set(iteratorName) { i.toDouble() }
			result = body.get(context)
		}
		return result
	}

	override fun isMutable(): Boolean = array.isMutable() || body.isMutable()
	override fun toString(): String = "for_each($variableName, $array, $body)"
}
