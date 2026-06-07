package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang AST 节点: 块表达式 { statements... }，返回最后一条语句的值
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

class BlockExpr(val body: MolangValue) : MolangValue {
	override fun get(context: MolangData?): Double = body.get(context)

	override fun isMutable(): Boolean = body.isMutable()

	override fun toString(): String = "{ $body }"
}
