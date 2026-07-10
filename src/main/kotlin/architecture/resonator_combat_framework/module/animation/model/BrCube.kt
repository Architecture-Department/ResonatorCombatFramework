package architecture.resonator_combat_framework.module.animation.model

import org.joml.Vector3f

/**
 * 运行时立方体数据。
 *
 * @property inflate 膨胀值
 * @property origin 原点坐标
 * @property size 尺寸
 * @property rotation 旋转
 */
data class BrCube
@JvmOverloads
constructor(
	val inflate: Float = 0f,
	val origin: Vector3f = Vector3f(),
	val size: Vector3f = Vector3f(),
	val rotation: Vector3f = Vector3f()
) {
	companion object {
		/**
		 * 从 [BakingBrCube] 转换为运行时 [BrCube]。
		 *
		 * @param cube 烘培立方体数据
		 * @return 运行时立方体实例
		 */
		@JvmStatic
		fun of(cube: BakingBrCube): BrCube {
			return BrCube(
				cube.inflate,
				Vector3f(cube.origin),
				Vector3f(cube.size),
				Vector3f(cube.rotation)
			)
		}
	}
}