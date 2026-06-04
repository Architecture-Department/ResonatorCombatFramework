// MoLang 数学函数接口。所有 math.xxx 函数通过 Factory 模式注册到 MathParser
package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

interface MolangFunction : MolangValue {
	fun interface Factory<T : MolangFunction> {
		fun create(vararg args: MolangValue): T
	}
}

