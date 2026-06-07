package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang AST 节点: continue 语句——跳至当前循环的下一次迭代
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

object ContinueExpr : MolangValue {
	override fun get(context: MolangData?): Double = 0.0

	override fun isMutable(): Boolean = false

	override fun toString(): String = "continue"
}
