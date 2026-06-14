package architecture.resonator_combat_framework.module.entity_animation.animation.molang.value

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang AST 节点: return 语句——返回表达式值并退出当前块

class ReturnExpr(val value: MolangValue) : MolangValue {
	override fun eval(context: MolangData?): Double = value.eval(context)

	override fun isMutable(): Boolean = false

	override fun toString(): String = "return $value"
}
