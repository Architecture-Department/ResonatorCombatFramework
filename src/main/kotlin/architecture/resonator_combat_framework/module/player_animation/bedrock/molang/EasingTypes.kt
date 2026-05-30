package architecture.resonator_combat_framework.module.player_animation.bedrock.molang

/** 缓动函数类型：Double → Double */
typealias EasingFunc = (Double) -> Double

object EasingTypes {
	val LINEAR: EasingFunc = { t -> t }
	val STEP: EasingFunc = { t -> if (t < 1.0) 0.0 else 1.0 }

	/** 标准四点 Catmull-Rom 样条 */
	fun catmullRom(t: Double, p0: Double, p1: Double, p2: Double, p3: Double): Double {
		val v0 = (p2 - p0) * 0.5
		val v1 = (p3 - p1) * 0.5
		val t2 = t * t
		val t3 = t2 * t
		return (2.0 * p1 - 2.0 * p2 + v0 + v1) * t3 +
			(-3.0 * p1 + 3.0 * p2 - 2.0 * v0 - v1) * t2 +
			v0 * t + p1
	}
}