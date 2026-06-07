package architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.random

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang 函数: math.die_roll(sides, rolls) — 骰子投掷（浮点和）
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MolangFunction

class DieRollFunction(private val sides: MolangValue, private val rolls: MolangValue) : MolangFunction {
	override fun get(context: MolangData?): Double {
		val s = sides.get(context)
		val r = rolls.get(context)
		var sum = 0.0
		for (i in 0..<r.toInt()) sum += (1 + (Math.random() * s).toInt()).toDouble()
		return sum
	}

	override fun isMutable(): Boolean {
		return true
	}
}

