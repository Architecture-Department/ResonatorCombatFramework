package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang AST 节点: return 语句——返回表达式值并退出当前块
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

class ReturnExpr(val value: MolangValue) : MolangValue {
	override fun get(context: MolangData?): Double = value.get(context)

	override fun isMutable(): Boolean = false

	override fun toString(): String = "return $value"
}
