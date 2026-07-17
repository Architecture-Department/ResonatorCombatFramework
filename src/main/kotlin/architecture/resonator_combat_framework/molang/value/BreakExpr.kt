package architecture.resonator_combat_framework.molang.value

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang AST 节点: break 语句——退出当前循环

object BreakExpr : MolangValue {
	override fun eval(context: MolangDataHolder?): Double = 0.0

	override fun isMutable(): Boolean = false

	override fun toString(): String = "break"
}
