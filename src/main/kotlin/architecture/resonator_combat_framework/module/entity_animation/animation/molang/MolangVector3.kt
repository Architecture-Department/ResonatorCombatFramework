package architecture.resonator_combat_framework.module.entity_animation.animation.molang

import org.joml.Vector3f

/** 每个轴可独立为数字或 MoLang 表达式，null 表示"无数据" */
data class MolangVector3(
	val x: MolangValue? = null,
	val y: MolangValue? = null,
	val z: MolangValue? = null
) {
	fun evaluate(out: Vector3f = Vector3f(), context: MolangData? = null): Vector3f {
		return out.set(
			x?.get(context)?.toFloat() ?: 0f,
			y?.get(context)?.toFloat() ?: 0f,
			z?.get(context)?.toFloat() ?: 0f
		)
	}

	fun allNull(): Boolean = x == null && y == null && z == null
}