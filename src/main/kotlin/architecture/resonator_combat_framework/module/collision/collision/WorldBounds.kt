package architecture.resonator_combat_framework.module.collision.collision

/**
 * 碰撞体的世界空间包围球信息，用于构建搜索 AABB。
 */
data class WorldBounds(
	val sphereRadius: Float,
	val cx: Double,
	val cy: Double,
	val cz: Double,
)