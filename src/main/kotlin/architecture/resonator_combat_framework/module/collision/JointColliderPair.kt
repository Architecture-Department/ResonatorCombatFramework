package architecture.resonator_combat_framework.module.collision

/**
 * 骨骼-碰撞体绑定对。
 *
 * @param boneName 骨骼名称（对应 [ProxyModel] 中的骨骼名）
 * @param collider 附着于该骨骼的碰撞体
 */
data class JointColliderPair(
	val boneName: String,
	val collider: AnimCollider,
)
