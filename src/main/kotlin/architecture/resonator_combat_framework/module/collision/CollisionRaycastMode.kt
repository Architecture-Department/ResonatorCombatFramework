package architecture.resonator_combat_framework.module.collision

/**
 * 碰撞射线检查模式 —— 控制碰撞检测后是否用射线验证遮挡。
 *
 * 防止碰撞体穿透方块打到目标。
 */
enum class CollisionRaycastMode {
	/** 不进行射线检查 */
	NONE,

	/** 从碰撞体中心发射射线到目标中心 */
	FROM_COLLIDER,

	/** 从实体位置发射射线到目标中心 */
	FROM_ENTITY,
}
