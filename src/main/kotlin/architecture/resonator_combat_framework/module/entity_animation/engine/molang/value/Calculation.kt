package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang AST 节点: 二元运算（如 a + b），带不可变节点缓存优化
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.Operator

class Calculation(
	private val operator: Operator,
	private val argA: MolangValue,
	private val argB: MolangValue
) : MolangValue {
	private val isMutable: Boolean = argA.isMutable() || argB.isMutable()
	private var cachedValue = Double.MIN_VALUE

	override fun get(context: MolangData?): Double {
		if (isMutable) {
			return operator.compute(argA.get(context), argB.get(context))
		}
		if (cachedValue == Double.MIN_VALUE) {
			cachedValue = operator.compute(argA.get(context), argB.get(context))
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

