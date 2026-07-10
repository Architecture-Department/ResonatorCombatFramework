package architecture.resonator_combat_framework.module.animation.molang.value

import architecture.resonator_combat_framework.module.animation.molang.MolangData
import architecture.resonator_combat_framework.module.animation.molang.MolangValue
import architecture.resonator_combat_framework.module.animation.molang.Operator

/**
 * MoLang AST 节点 —— 二元运算表达式（如 a + b, a > b 等）。
 * 不可变子表达式的结果会被缓存以避免重复计算。
 */
class Calculation(
	private val operator: Operator,
	private val argA: MolangValue,
	private val argB: MolangValue
) : MolangValue {
	private val isMutable: Boolean = argA.isMutable() || argB.isMutable()
	private var cachedValue = Double.MIN_VALUE

	override fun eval(context: MolangData?): Double {
		if (isMutable) {
			return operator.compute(argA.eval(context), argB.eval(context))
		}
		if (cachedValue == Double.MIN_VALUE) {
			cachedValue = operator.compute(argA.eval(context), argB.eval(context))
		}
		return cachedValue
	}

	override fun isMutable(): Boolean {
		return isMutable
	}

	fun operator(): Operator {
		return operator
	}

	fun argA(): MolangValue {
		return argA
	}

	fun argB(): MolangValue {
		return argB
	}

	override fun toString(): String {
		return "$argA ${operator.symbol} $argB"
	}
}

