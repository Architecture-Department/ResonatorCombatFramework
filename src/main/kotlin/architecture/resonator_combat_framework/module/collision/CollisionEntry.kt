package architecture.resonator_combat_framework.module.collision

import architecture.resonator_combat_framework.module.collision.collision.CollisionShape
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix4f

/**
 * 碰撞条目 —— 一次碰撞检测的完整描述。
 *
 * 纯数据类，不包含任何游戏逻辑（伤害、击退、回调等）。
 * 游戏逻辑由 [architecture.resonator_combat_framework.module.collision.event.CollisionHitEvent] 的监听者处理。
 *
 * @property id          唯一标识。用于投递 [CollisionHitEvent] 时区分碰撞来源。
 * @property groupId     命中记录分组 ID。同一分组的碰撞体共享命中记录（防重复命中），
 *                       默认与 [id] 相同。同一个动画的所有阶段碰撞体应使用同一 [groupId]。
 * @property shape       碰撞形状（相对坐标）。
 * @property worldMatrix 预计算的世界空间变换矩阵（含实体坐标 + 骨骼层次）。
 *                       为 null 时使用相对坐标 + 实体位置直接变换。
 * @property hasEffect   是否触发碰撞
 * @property raycastMode 射线遮挡检查模式。
 */
data class CollisionEntry(
	val id: ResourceLocation,
	var shape: CollisionShape,
	var worldMatrix: Matrix4f? = null,
	var hasEffect: Boolean = true,
	var raycastMode: CollisionRaycastMode = CollisionRaycastMode.NONE,
	val groupId: ResourceLocation = id,
)
