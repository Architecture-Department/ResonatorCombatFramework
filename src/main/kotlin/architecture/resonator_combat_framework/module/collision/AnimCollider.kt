package architecture.resonator_combat_framework.module.collision

import net.minecraft.world.phys.AABB
import org.joml.Matrix4fc
import org.joml.Vector3f
import kotlin.math.abs

/**
 * 动画碰撞体 —— 定义附着于骨骼上的 OBB（有向包围盒）。
 *
 * @param center      碰撞体中心相对于骨骼原点的偏移（模型坐标）
 * @param halfExtents 碰撞体半边长（模型坐标）
 */
data class AnimCollider(
	val center: Vector3f,
	val halfExtents: Vector3f,
) {
	companion object {
		fun obbAabbOverlap(boneMatrix: Matrix4fc, collider: AnimCollider, entityBox: AABB): Boolean {
			val axisX = Vector3f(boneMatrix.m00(), boneMatrix.m10(), boneMatrix.m20())
			val axisY = Vector3f(boneMatrix.m01(), boneMatrix.m11(), boneMatrix.m21())
			val axisZ = Vector3f(boneMatrix.m02(), boneMatrix.m12(), boneMatrix.m22())
			val lenX = axisX.length()
			val lenY = axisY.length()
			val lenZ = axisZ.length()
			axisX.div(lenX); axisY.div(lenY); axisZ.div(lenZ)

			val h = collider.halfExtents
			val obbHalfX = lenX * h.x
			val obbHalfY = lenY * h.y
			val obbHalfZ = lenZ * h.z
			val c = collider.center
			val obbCenter = Vector3f(boneMatrix.m30(), boneMatrix.m31(), boneMatrix.m32())
			obbCenter.add(Vector3f(axisX).mul(c.x)).add(Vector3f(axisY).mul(c.y)).add(Vector3f(axisZ).mul(c.z))

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

		/** 检查两个 AABB 是否重叠（快速粗筛用） */
		fun aabbOverlap(a: AABB, b: AABB): Boolean {
			return a.maxX >= b.minX && a.minX <= b.maxX &&
				a.maxY >= b.minY && a.minY <= b.maxY &&
				a.maxZ >= b.minZ && a.minZ <= b.maxZ
		}
	}
}
