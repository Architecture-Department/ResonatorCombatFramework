package architecture.resonator_combat_framework.module.collision.collision

import architecture.resonator_combat_framework.module.collision.CollisionEntry
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import org.joml.Matrix4fc
import org.joml.Vector3f
import kotlin.math.abs

/**
 * OBB（有向包围盒），可绑定到骨骼。
 *
 * 继承 [OrientedBox] 作为中间基类，实现 [CollisionShape] 接口。
 *
 * @property center      碰撞体中心相对骨骼原点（或实体原点）的偏移
 * @property halfExtents 碰撞体半边长（各轴方向）
 */
data class OBB(
	override val center: Vector3f,
	override val halfExtents: Vector3f,
) : OrientedBox(center, halfExtents), CollisionShape {

	/**
	 * 返回缩放后的 OBB。
	 *
	 * @param factor 各轴缩放因子
	 * @return 缩放后的自身引用（链式调用）
	 */
	fun scaled(factor: Vector3f): OBB = apply { halfExtents.mul(factor) }

	/**
	 * 返回平移后的 OBB。
	 *
	 * @param offset 各轴偏移量
	 * @return 平移后的自身引用（链式调用）
	 */
	fun translated(offset: Vector3f): OBB = apply { center.add(offset) }

	override fun checkCollision(entry: CollisionEntry, attacker: Entity, targetBox: AABB): Boolean {
		if (isSphereFar(entry, attacker, targetBox)) return false

		if (entry.worldMatrix != null) {
			return obbAabbOverlap(entry.worldMatrix!!, this, targetBox)
		}
		return true
	}

	companion object {
		/**
		 * 对 OBB 与 AABB 执行 SAT 碰撞检测。
		 *
		 * 测试 3 个 OBB 轴 + 3 个 AABB 轴 + 9 条叉积轴，共 15 条分离轴。
		 *
		 * @param boneMatrix OBB 的世界变换矩阵（含骨骼变换）
		 * @param obb        OBB 碰撞体
		 * @param entityBox  目标实体的 AABB
		 * @return true 表示 OBB 与 AABB 相交
		 */
		@JvmStatic
		fun obbAabbOverlap(boneMatrix: Matrix4fc, obb: OBB, entityBox: AABB): Boolean =
			obbAabbOverlap(boneMatrix, obb.center, obb.halfExtents, entityBox)

		/**
		 * OBB 与 AABB 的 SAT 碰撞检测核心实现。
		 *
		 * 从变换矩阵提取 OBB 的轴向量和半边长，在 15 条分离轴上投影，
		 * 检验是否存在任意一条分离轴使两包围盒分离。
		 *
		 * @param boneMatrix  世界变换矩阵
		 * @param center      OBB 局部中心坐标
		 * @param halfExtents OBB 局部半边长
		 * @param entityBox   目标实体的 AABB
		 * @return true 表示相交
		 */
		@JvmStatic
		fun obbAabbOverlap(boneMatrix: Matrix4fc, center: Vector3f, halfExtents: Vector3f, entityBox: AABB): Boolean {
			val axisX = Vector3f(boneMatrix.m00(), boneMatrix.m10(), boneMatrix.m20())
			val axisY = Vector3f(boneMatrix.m01(), boneMatrix.m11(), boneMatrix.m21())
			val axisZ = Vector3f(boneMatrix.m02(), boneMatrix.m12(), boneMatrix.m22())
			val lenX = axisX.length()
			val lenY = axisY.length()
			val lenZ = axisZ.length()
			axisX.div(lenX); axisY.div(lenY); axisZ.div(lenZ)

			val obbHalfX = lenX * halfExtents.x
			val obbHalfY = lenY * halfExtents.y
			val obbHalfZ = lenZ * halfExtents.z
			val obbCenter = Vector3f(boneMatrix.m30(), boneMatrix.m31(), boneMatrix.m32())
			obbCenter.add(Vector3f(axisX).mul(center.x)).add(Vector3f(axisY).mul(center.y))
				.add(Vector3f(axisZ).mul(center.z))

			val aabbCenter = Vector3f(
				((entityBox.minX + entityBox.maxX) * 0.5f).toFloat(),
				((entityBox.minY + entityBox.maxY) * 0.5f).toFloat(),
				((entityBox.minZ + entityBox.maxZ) * 0.5f).toFloat(),
			)
			val aabbHalf = Vector3f(
				((entityBox.maxX - entityBox.minX) * 0.5f).toFloat(),
				((entityBox.maxY - entityBox.minY) * 0.5f).toFloat(),
				((entityBox.maxZ - entityBox.minZ) * 0.5f).toFloat(),
			)
			val toCenter = Vector3f(obbCenter).sub(aabbCenter)
			val obbAxes = arrayOf(axisX, axisY, axisZ)
			val obbHalfs = floatArrayOf(obbHalfX, obbHalfY, obbHalfZ)
			val aabbEdgeAxes = arrayOf(Vector3f(1f, 0f, 0f), Vector3f(0f, 1f, 0f), Vector3f(0f, 0f, 1f))

			for (i in 0..2) {
				if (!testSat(aabbEdgeAxes[i], toCenter, obbAxes, obbHalfs, aabbHalf)) return false
			}
			for (i in 0..2) {
				if (!testSat(obbAxes[i], toCenter, obbAxes, obbHalfs, aabbHalf)) return false
			}
			for (i in 0..2) {
				for (j in 0..2) {
					val cross = Vector3f(obbAxes[i]).cross(aabbEdgeAxes[j])
					if (cross.length() < 1e-6f) continue
					cross.normalize()
					if (!testSat(cross, toCenter, obbAxes, obbHalfs, aabbHalf)) return false
				}
			}
			return true
		}

		/**
		 * SAT 分离轴测试 —— 在给定轴上检验 OBB 与 AABB 的投影是否重叠。
		 *
		 * @param axis     分离轴
		 * @param toCenter OBB 中心到 AABB 中心的向量
		 * @param obbAxes  OBB 的三个轴向
		 * @param obbHalfs OBB 的三个半边长
		 * @param aabbHalf AABB 的三轴半边长
		 * @return true 表示在该轴上重叠
		 */
		private fun testSat(
			axis: Vector3f, toCenter: Vector3f,
			obbAxes: Array<Vector3f>, obbHalfs: FloatArray, aabbHalf: Vector3f,
		): Boolean {
			val projCenter = abs(axis.dot(toCenter))
			var projOBB = 0f
			for (k in 0..2) projOBB += abs(axis.dot(obbAxes[k])) * obbHalfs[k]
			val projAABB = abs(axis.x) * aabbHalf.x + abs(axis.y) * aabbHalf.y + abs(axis.z) * aabbHalf.z
			return projCenter <= projOBB + projAABB + 1e-8f
		}
	}
}
