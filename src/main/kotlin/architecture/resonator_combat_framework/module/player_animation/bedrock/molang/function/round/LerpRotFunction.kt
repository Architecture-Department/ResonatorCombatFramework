// MoLang 函数: math.lerprotate(a, b, t) — 角度线性插值
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.round

import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.MathValue
import architecture.resonator_combat_framework.module.player_animation.bedrock.molang.function.MathFunction

class LerpRotFunction(private val a: MathValue, private val b: MathValue, private val t: MathValue) : MathFunction {
	override fun get(): Double {
		val va = a.get()
		val vb = b.get()
		val vt = t.get()
		val diff = ((vb - va) % 360 + 540) % 360 - 180
		return va + diff * vt
	}

	override fun isMutable(): Boolean {
		return a.isMutable() || b.isMutable() || t.isMutable()
	}
}



