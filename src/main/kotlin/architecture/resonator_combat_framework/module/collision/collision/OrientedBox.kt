package architecture.resonator_combat_framework.module.collision.collision

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.collision.CollisionEntry
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import org.joml.Vector3f
import kotlin.math.sqrt

/**
 * OBB 系列的中间抽象基类。
 *
 * 提供 [center] 和 [halfExtents] 公共字段，
 * [OBB]（保持 data class）和 [BoneCollider] 均继承此类。
 * 此类实现 [CollisionShape.computeWorldBounds] 的默认逻辑。
 *
 * @property center      碰撞体中心相对骨骼/实体原点的偏移
 * @property halfExtents 碰撞体半边长
 */
@AllOpe
abstract class OrientedBox(
	val center: Vector3f,
	val halfExtents: Vector3f,
) : CollisionShape {

	override fun computeWorldBounds(entry: CollisionEntry, attacker: Entity): WorldBounds {
		val h = halfExtents
		val sphereRadius = sqrt(h.x * h.x + h.y * h.y + h.z * h.z)
		val m = entry.worldMatrix
		val cx = m?.m30()?.toDouble() ?: (attacker.x + center.x.toDouble())
		val cy = m?.m31()?.toDouble() ?: (attacker.y + center.y.toDouble())
		val cz = m?.m32()?.toDouble() ?: (attacker.z + center.z.toDouble())
		return WorldBounds(sphereRadius, cx, cy, cz)
	}

	/**
	 * 球体提前退出检测。
	 *
	 * 在完整 SAT 检测前，先判断两包围球是否相交。
	 * 若不相交则直接返回 true，跳过昂贵的 SAT 计算。
	 *
	 * @param entry    碰撞条目
	 * @param attacker 攻击者实体
	 * @param targetBox 目标实体的 AABB
	 * @return true 表示两包围球不相交，SAT 可跳过
	 */
	protected fun isSphereFar(
		entry: CollisionEntry,
		attacker: Entity,
		targetBox: AABB
	): Boolean {
		val h = halfExtents
		val sphereRadius = sqrt(h.x * h.x + h.y * h.y + h.z * h.z)

		val shapeCenter = if (entry.worldMatrix != null) {
			Vector3f(entry.worldMatrix!!.m30(), entry.worldMatrix!!.m31(), entry.worldMatrix!!.m32())
		} else Vector3f(
			(attacker.x + center.x).toFloat(),
			(attacker.y + center.y).toFloat(),
			(attacker.z + center.z).toFloat(),
		)

		val targetCenter = Vector3f(
			((targetBox.minX + targetBox.maxX) * 0.5).toFloat(),
			((targetBox.minY + targetBox.maxY) * 0.5).toFloat(),
			((targetBox.minZ + targetBox.maxZ) * 0.5).toFloat(),
		)

		val targetRadius = sqrt(
			((targetBox.maxX - targetBox.minX) * 0.5).let { it * it } +
				((targetBox.maxY - targetBox.minY) * 0.5).let { it * it } +
				((targetBox.maxZ - targetBox.minZ) * 0.5).let { it * it }
		).toFloat()

		if (shapeCenter.distance(targetCenter) > sphereRadius + targetRadius) return true
		return false
	}
}
