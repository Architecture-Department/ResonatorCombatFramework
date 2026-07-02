package architecture.resonator_combat_framework.module.collision.collision

import architecture.resonator_combat_framework.module.collision.CollisionEntry
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import org.joml.Matrix4f
import org.joml.Vector3f

/**
 * 骨骼碰撞体 —— 在 [OBB] 基础上记录所属骨骼和局部旋转。
 *
 * 世界坐标计算：实体位置 + 骨骼全局变换 + [center] 偏移 + [rotation] 旋转（ZYX 顺序）。
 *
 * @property boneName     所属骨骼名称，用于从模型中查找骨骼变换矩阵
 * @property center       相对骨骼原点的偏移
 * @property halfExtents  碰撞体半边长
 * @property rotation     相对骨骼的局部旋转（度），ZYX 欧拉角顺序
 */
open class BoneCollider(
	val boneName: String,
	center: Vector3f,
	halfExtents: Vector3f,
	val rotation: Vector3f = Vector3f(),
) : OrientedBox(center, halfExtents) {

	override fun checkCollision(entry: CollisionEntry, attacker: Entity, targetBox: AABB): Boolean {
		if (isSphereFar(entry, attacker, targetBox)) return false

		if (entry.worldMatrix != null) {
			// 将局部旋转合成到世界矩阵中，再执行 SAT 检测
			val rotMatrix = Matrix4f()
				.rotateZ(rotation.z() * (Math.PI.toFloat() / 180f))
				.rotateY(rotation.y() * (Math.PI.toFloat() / 180f))
				.rotateX(rotation.x() * (Math.PI.toFloat() / 180f))
			val combined = Matrix4f(entry.worldMatrix).mul(rotMatrix)
			return OBB.obbAabbOverlap(combined, center, halfExtents, targetBox)
		}
		return true
	}
}
