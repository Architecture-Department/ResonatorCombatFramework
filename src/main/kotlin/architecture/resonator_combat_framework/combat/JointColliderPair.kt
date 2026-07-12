package architecture.resonator_combat_framework.combat

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.collision.OBBCollider

/**
 * 骨骼-碰撞体绑定对。
 *
 * 对应 Epic Fight 的 Phase.collider + colliderJoint 组合，
 * 将碰撞体附加到指定骨骼上，在攻击动画播放时跟随骨骼运动。
 *
 * @param boneName 目标骨骼名称（如 "right_item"）
 * @param collider OBB 碰撞器
 */
@AllOpe
data class JointColliderPair(
	val boneName: String,
	val collider: OBBCollider,
)
