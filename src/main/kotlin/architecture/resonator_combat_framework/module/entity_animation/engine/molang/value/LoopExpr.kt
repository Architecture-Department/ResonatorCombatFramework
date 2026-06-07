package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang AST 节点: loop(count, expression) — 循环求值 expression count 次，返回最后一次结果
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

class LoopExpr(val count: MolangValue, val body: MolangValue) : MolangValue {
	override fun get(context: MolangData?): Double {
		val iterations = count.get(context).toInt().coerceAtLeast(0)
		var result = 0.0
		for (i in 0 until iterations) {
			result = body.get(context)
		}
		return result
	}

	override fun isMutable(): Boolean = count.isMutable() || body.isMutable()

	override fun toString(): String = "loop($count, $body)"
}
