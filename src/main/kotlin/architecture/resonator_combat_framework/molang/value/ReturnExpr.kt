package architecture.resonator_combat_framework.molang.value

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang AST 节点: return 语句——返回表达式值并退出当前块

class ReturnExpr(val value: MolangValue) : MolangValue {
	override fun eval(context: MolangDataHolder?): Double = value.eval(context)

	override fun isMutable(): Boolean = false

	override fun toString(): String = "return $value"
}
