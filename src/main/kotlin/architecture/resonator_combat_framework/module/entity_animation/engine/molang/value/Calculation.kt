// MoLang AST 节点: 二元运算（如 a + b），带不可变节点缓存优化
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.Operator

class Calculation(
	private val operator: Operator,
	private val argA: MathValue,
	private val argB: MathValue
) : MathValue {
	private val isMutable: Boolean = argA.isMutable() || argB.isMutable()
	private var cachedValue = Double.MIN_VALUE

	override fun get(): Double {
		if (isMutable) {
			return operator.compute(argA.get(), argB.get())
		}
		if (cachedValue == Double.MIN_VALUE) {
			cachedValue = operator.compute(argA.get(), argB.get())
		}
		return cachedValue
	}

	override fun isMutable(): Boolean {
		return isMutable
	}

	fun operator(): Operator {
		return operator
	}

	fun argA(): MathValue {
		return argA
	}

	fun argB(): MathValue {
		return argB
	}

	override fun toString(): String {
		return "$argA ${operator.symbol} $argB"
	}
}

