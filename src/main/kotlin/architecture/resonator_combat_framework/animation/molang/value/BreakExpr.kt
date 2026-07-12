package architecture.resonator_combat_framework.animation.molang.value

import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue

// MoLang AST 节点: break 语句——退出当前循环

object BreakExpr : MolangValue {
	override fun eval(context: MolangData?): Double = 0.0

	override fun isMutable(): Boolean = false

	override fun toString(): String = "break"
}
