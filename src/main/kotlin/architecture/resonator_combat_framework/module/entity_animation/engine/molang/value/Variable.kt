// MoLang AST 节点: 变量/查询引用，通过 AtomicReference<DoubleSupplier> 实现运行时动态求值
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathValue
import java.util.concurrent.atomic.AtomicReference
import java.util.function.DoubleSupplier

class Variable(
	val name: String,
	val value: AtomicReference<DoubleSupplier> = AtomicReference(DoubleSupplier { 0.0 })
) : MathValue {

	constructor(name: String, initialValue: Double) : this(
		name,
		AtomicReference(DoubleSupplier { initialValue })
	)

	constructor(name: String, initialValue: DoubleSupplier) : this(
		name,
		AtomicReference(initialValue)
	)

	override fun get(): Double {
		return value.get().asDouble
	}

	fun set(newValue: DoubleSupplier) {
		value.set(newValue)
	}

	fun set(newValue: Double) {
		value.set(DoubleSupplier { newValue })
	}

	override fun isMutable(): Boolean = true

	override fun toString(): String {
		return "$name:${get()}"
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Variable) return false
		return name == other.name
	}

	override fun hashCode(): Int = name.hashCode()
}

