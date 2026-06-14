package architecture.resonator_combat_framework.module.entity_animation.animation.molang.value

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang AST 节点: 块表达式 { statements... }，返回最后一条语句的值

class BlockExpr(val body: MolangValue) : MolangValue {
	override fun eval(context: MolangData?): Double = body.eval(context)

	override fun isMutable(): Boolean = body.isMutable()

	override fun toString(): String = "{ $body }"
}
