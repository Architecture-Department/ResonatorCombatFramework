package architecture.resonator_combat_framework.combat

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.collision.collision.BoneCollider
import architecture.resonator_combat_framework.module.collision.collision.CollisionShape
import org.joml.Vector3f

/**
 * 骨骼-碰撞体绑定对。
 *
 * @param boneName 骨骼名称（对应 [ProxyModel] 中的骨骼名）
 * @param collider 附着于该骨骼的碰撞体，可以是 [OBB] 或 [BoneCollider]
 */
@AllOpe
data class JointColliderPair(
	val boneName: String,
	val collider: CollisionShape,
) {
	companion object {
		@JvmStatic
		fun of(
			boneName: String,
			center: Vector3f = Vector3f(),
			halfExtents: Vector3f = Vector3f(),
			rotation: Vector3f = Vector3f()
		): JointColliderPair {
			return JointColliderPair(boneName, BoneCollider(center, halfExtents, rotation))
		}
	}
}