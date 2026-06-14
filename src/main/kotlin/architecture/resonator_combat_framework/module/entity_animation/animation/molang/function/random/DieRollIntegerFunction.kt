package architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.random

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue

// MoLang 函数: math.die_roll_integer(sides, rolls) — 骰子投掷（整数和）
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.function.MolangFunction

class DieRollIntegerFunction(private val sides: MolangValue, private val rolls: MolangValue) : MolangFunction {
	override fun eval(context: MolangData?): Double {
		val s = sides.eval(context)
		val r = rolls.eval(context)
		var sum = 0.0
		for (i in 0..<r.toInt()) sum += (1 + (Math.random() * s).toInt()).toDouble()
		return sum.toInt().toDouble()
	}

	override fun isMutable(): Boolean {
		return true
	}
}

