// MoLang AST 节点: 变量赋值 variable = expr
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.value

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import java.util.function.DoubleSupplier

class VariableAssignment(private val variable: Variable, private val value: MathValue) : MathValue {
	override fun get(): Double {
		val result = value.get()
		variable.set(DoubleSupplier { result })
		return result
	}

	override fun isMutable(): Boolean {
		return true
	}

	override fun toString(): String {
		return variable.name + " = " + value
	}
}



