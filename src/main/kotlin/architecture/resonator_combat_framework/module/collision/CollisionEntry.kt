package architecture.resonator_combat_framework.module.collision

import net.minecraft.resources.ResourceLocation
import org.joml.Matrix4f

/**
 * 碰撞条目 —— 一次碰撞检测的完整描述。
 *
 * 纯数据类，不包含任何游戏逻辑（伤害、击退、回调等）。
 * 游戏逻辑由 [architecture.resonator_combat_framework.module.collision.event.CollisionHitEvent] 的监听者处理。
 *
 * @property id          唯一标识。用于命中追踪（同一次挥击不重复命中同一实体）。
 * @property shape       碰撞形状（相对坐标）。
 * @property worldMatrix 预计算的世界空间变换矩阵（含实体坐标 + 骨骼层次）。
 *                       为 null 时使用相对坐标 + 实体位置直接变换。
 * @property expiryTick  过期游戏刻。到达此值时自动清除，默认永不超时。
 */
data class CollisionEntry(
	val id: ResourceLocation,
	val shape: CollisionShape,
	val worldMatrix: Matrix4f? = null,
	val expiryTick: Long = Long.MAX_VALUE,
)
