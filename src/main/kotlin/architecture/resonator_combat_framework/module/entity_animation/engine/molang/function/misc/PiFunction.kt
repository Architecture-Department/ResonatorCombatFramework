package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.misc

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang 函数: math.pi() — 圆周率常数
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction

class PiFunction : MolangFunction {
	override fun get(context: MolangData?): Double {
		return Math.PI
	}

	override fun isMutable(): Boolean {
		return false
	}
}

