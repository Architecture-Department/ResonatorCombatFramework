package architecture.resonator_combat_framework.module.entity_animation.animation.molang.value

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang AST 节点: loop(count, expression) — 循环求值 expression count 次，返回最后一次结果

class LoopExpr(val count: MolangValue, val body: MolangValue) : MolangValue {
	override fun eval(context: MolangData?): Double {
		val iterations = count.eval(context).toInt().coerceAtLeast(0)
		var result = 0.0
		for (i in 0 until iterations) {
			result = body.eval(context)
		}
		return result
	}

	override fun isMutable(): Boolean = count.isMutable() || body.isMutable()

	override fun toString(): String = "loop($count, $body)"
}
