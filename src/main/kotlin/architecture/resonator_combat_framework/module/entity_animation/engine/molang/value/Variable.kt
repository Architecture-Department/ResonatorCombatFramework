// MoLang AST 节点: 变量/查询引用，通过 MoLang 作用域链运行时动态求值
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MoLang
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import java.util.function.DoubleSupplier

class Variable(val name: String) : MolangValue {

	override fun get(): Double {
		return MoLang.resolve(name)
	}

	override fun isMutable(): Boolean = true

	fun set(supplier: DoubleSupplier) {
		MoLang.set(name, supplier)
	}

	fun set(value: Double) {
		MoLang.assign(name, value)
	}

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
