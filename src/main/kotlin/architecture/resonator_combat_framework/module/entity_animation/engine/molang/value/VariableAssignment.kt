// MoLang AST 节点: 变量赋值 variable = expr，通过 MoLang 作用域链写入
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MoLang
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

class VariableAssignment(private val variable: Variable, private val value: MolangValue) : MolangValue {
	override fun get(): Double {
		val result = value.get()
		MoLang.assign(variable.name, result)
		return result
	}

	override fun isMutable(): Boolean {
		return true
	}

	override fun toString(): String {
		return variable.name + " = " + value
	}
}
