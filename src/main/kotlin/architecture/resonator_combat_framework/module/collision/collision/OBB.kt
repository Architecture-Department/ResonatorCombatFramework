package architecture.resonator_combat_framework.module.collision.collision

import architecture.resonator_combat_framework.module.collision.CollisionEntry
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import org.joml.Matrix4fc
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * OBB（有向包围盒），可绑定到骨骼。
 *
 * @param center     碰撞体中心相对骨骼原点（或实体原点）的偏移
 * @param halfExtents 碰撞体半边长
 */
data class OBB(
	val center: Vector3f,
	val halfExtents: Vector3f,
) : CollisionShape {

	/** 返回缩放后的 OBB */
	fun scaled(factor: Vector3f): OBB = apply { halfExtents.mul(factor) }

	/** 返回平移后的 OBB */
	fun translated(offset: Vector3f): OBB = apply { center.add(offset) }

	override fun checkCollision(entry: CollisionEntry, attacker: Entity, targetBox: AABB): Boolean {
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

		if (shapeCenter.distance(targetCenter) > sphereRadius + targetRadius) return false

		if (entry.worldMatrix != null) {
			return obbAabbOverlap(entry.worldMatrix!!, this, targetBox)
		}
		return true
	}

	override fun computeWorldBounds(entry: CollisionEntry, attacker: Entity): WorldBounds {
		val h = halfExtents
		val sphereRadius = sqrt(h.x * h.x + h.y * h.y + h.z * h.z)
		val m = entry.worldMatrix
		val cx = m?.m30()?.toDouble() ?: (attacker.x + center.x.toDouble())
		val cy = m?.m31()?.toDouble() ?: (attacker.y + center.y.toDouble())
		val cz = m?.m32()?.toDouble() ?: (attacker.z + center.z.toDouble())
		return WorldBounds(sphereRadius, cx, cy, cz)
	}

	companion object {
		@JvmStatic
		fun obbAabbOverlap(boneMatrix: Matrix4fc, obb: OBB, entityBox: AABB): Boolean {
			val axisX = Vector3f(boneMatrix.m00(), boneMatrix.m10(), boneMatrix.m20())
			val axisY = Vector3f(boneMatrix.m01(), boneMatrix.m11(), boneMatrix.m21())
			val axisZ = Vector3f(boneMatrix.m02(), boneMatrix.m12(), boneMatrix.m22())
			val lenX = axisX.length()
			val lenY = axisY.length()
			val lenZ = axisZ.length()
			axisX.div(lenX); axisY.div(lenY); axisZ.div(lenZ)

			val h = obb.halfExtents
			val obbHalfX = lenX * h.x
			val obbHalfY = lenY * h.y
			val obbHalfZ = lenZ * h.z
			val c = obb.center
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
	}
}
