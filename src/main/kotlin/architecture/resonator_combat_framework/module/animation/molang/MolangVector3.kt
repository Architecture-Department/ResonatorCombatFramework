package architecture.resonator_combat_framework.module.animation.molang

import architecture.resonator_combat_framework.module.animation.molang.value.Constant
import architecture.resonator_combat_framework.module.animation.molang.value.Negative
import org.joml.Vector3f

/** 每个轴可独立为数字或 MoLang 表达式，null 表示"无数据" */
data class MolangVector3(
	val x: MolangValue? = null,
	val y: MolangValue? = null,
	val z: MolangValue? = null
) {
	fun evaluate(out: Vector3f = Vector3f(), context: MolangData? = null): Vector3f {
		return out.set(
			x?.eval(context)?.toFloat() ?: 0f,
			y?.eval(context)?.toFloat() ?: 0f,
			z?.eval(context)?.toFloat() ?: 0f
		)
	}

	fun allNull(): Boolean = x == null && y == null && z == null

	/**
	 * 镜像位置向量：X 取反，Y/Z 不变。
	 * @see AnimationMirrorUtil
	 */
	fun mirroredPos(): MolangVector3 = MolangVector3(
		x = negateMolang(x),
		y = y,
		z = z
	)

	/**
	 * 镜像旋转向量：Y 和 Z 取反，X 不变。
	 * @see AnimationMirrorUtil
	 */
	fun mirroredRot(): MolangVector3 = MolangVector3(
		x = x,
		y = negateMolang(y),
		z = negateMolang(z)
	)

	/**
	 * 如果 value 是 Constant 直接取反数值，否则用 Negative 包装表达式。
	 */
	private fun negateMolang(value: MolangValue?): MolangValue? {
		if (value == null) return null
		return if (value is Constant) Constant(-value.value) else Negative(value)
	}
}
