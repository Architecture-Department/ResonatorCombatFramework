// MoLang AST 节点接口。所有可求值的表达式节点都实现此接口，继承 java.util.function.DoubleSupplier
package architecture.resonator_combat_framework.module.entity_animation.engine.molang

import java.util.function.DoubleSupplier

interface MathValue : DoubleSupplier {
	fun get(): Double

	override fun getAsDouble(): Double = get()

	fun isMutable(): Boolean = true
}

