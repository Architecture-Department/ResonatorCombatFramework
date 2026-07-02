package architecture.resonator_combat_framework.module.collision.collision

/**
 * 碰撞体的世界空间包围球信息，用于构建搜索 AABB。
 *
 * @property sphereRadius 包围球半径
 * @property cx           包围球中心 X 坐标（世界坐标）
 * @property cy           包围球中心 Y 坐标（世界坐标）
 * @property cz           包围球中心 Z 坐标（世界坐标）
 */
data class WorldBounds(
	val sphereRadius: Float,
	val cx: Double,
	val cy: Double,
	val cz: Double,
)
