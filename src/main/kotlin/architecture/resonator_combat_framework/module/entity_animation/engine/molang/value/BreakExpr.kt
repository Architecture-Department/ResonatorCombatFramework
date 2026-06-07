package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang AST 节点: break 语句——退出当前循环
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

object BreakExpr : MolangValue {
	override fun get(context: MolangData?): Double = 0.0

	override fun isMutable(): Boolean = false

	override fun toString(): String = "break"
}
