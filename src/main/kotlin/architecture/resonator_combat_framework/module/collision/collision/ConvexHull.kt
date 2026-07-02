package architecture.resonator_combat_framework.module.collision.collision

import architecture.resonator_combat_framework.module.collision.CollisionEntry
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * ConvexHull（凸多边形碰撞体），可绑定到骨骼。
 *
 * 使用 SAT（分离轴定理）与目标实体的 AABB 进行碰撞检测。
 * 支持自定义顶点构成的任意凸多面体。
 *
 * @property boneName  绑定的骨骼名。不为 null 时，需要对应的 boneMatrix 才能变换到世界空间。
 *                      为 null 时使用实体位置直接变换。
 * @property vertices  定义凸多边形的顶点列表（模型坐标）。顶点需构成凸多面体。
 */
data class ConvexHull(
	val boneName: String? = null,
	val vertices: List<Vector3f>,
) : CollisionShape {

	override fun checkCollision(entry: CollisionEntry, attacker: Entity, targetBox: AABB): Boolean {
		val worldVerts = vertices.map { v ->
			if (entry.worldMatrix != null) {
				Vector3f(v).mulPosition(entry.worldMatrix)
			} else {
				Vector3f(
					(attacker.x + v.x).toFloat(),
					(attacker.y + v.y).toFloat(),
					(attacker.z + v.z).toFloat(),
				)
			}
		}

		val aabbCenter = Vector3f(
			((targetBox.minX + targetBox.maxX) * 0.5).toFloat(),
			((targetBox.minY + targetBox.maxY) * 0.5).toFloat(),
			((targetBox.minZ + targetBox.maxZ) * 0.5).toFloat(),
		)
		val aabbHalf = Vector3f(
			((targetBox.maxX - targetBox.minX) * 0.5).toFloat(),
			((targetBox.maxY - targetBox.minY) * 0.5).toFloat(),
			((targetBox.maxZ - targetBox.minZ) * 0.5).toFloat(),
		)

		if (!testAxis(worldVerts, aabbCenter, aabbHalf, Vector3f(1f, 0f, 0f))) return false
		if (!testAxis(worldVerts, aabbCenter, aabbHalf, Vector3f(0f, 1f, 0f))) return false
		if (!testAxis(worldVerts, aabbCenter, aabbHalf, Vector3f(0f, 0f, 1f))) return false

		val faceNormals = computeFaceNormals(worldVerts)
		for (normal in faceNormals) {
			if (!testAxis(worldVerts, aabbCenter, aabbHalf, normal)) return false
		}

		for ((i, element) in worldVerts.withIndex()) {
			for (j in i + 1 until worldVerts.size) {
				val edge = Vector3f(worldVerts[j]).sub(element)
				val len = edge.length()
				if (len < 1e-6f) continue
				edge.div(len)
				for (k in 0..2) {
					val aabbEdge = when (k) {
						0 -> Vector3f(1f, 0f, 0f); 1 -> Vector3f(0f, 1f, 0f); else -> Vector3f(0f, 0f, 1f)
					}
					val axis = Vector3f(edge).cross(aabbEdge)
					if (axis.length() < 1e-6f) continue
					axis.normalize()
					if (!testAxis(worldVerts, aabbCenter, aabbHalf, axis)) return false
				}
			}
		}

		return true
	}

	override fun computeWorldBounds(entry: CollisionEntry, attacker: Entity): WorldBounds {
		val m = entry.worldMatrix
		if (m != null) {
			val cx = m.m30().toDouble()
			val cy = m.m31().toDouble()
			val cz = m.m32().toDouble()
			val radiusSq = vertices.maxOf { v ->
				val wv = Vector3f(v).mulPosition(m)
				val dx = wv.x - m.m30()
				val dy = wv.y - m.m31()
				val dz = wv.z - m.m32()
				dx * dx + dy * dy + dz * dz
			}.toDouble()
			return WorldBounds(sqrt(radiusSq).toFloat(), cx, cy, cz)
		}
		val cx = attacker.x
		val cy = attacker.y
		val cz = attacker.z
		val radiusSq = vertices.maxOf { v ->
			val dx = v.x.toDouble()
			val dy = v.y.toDouble()
			val dz = v.z.toDouble()
			dx * dx + dy * dy + dz * dz
		}
		return WorldBounds(sqrt(radiusSq).toFloat(), cx, cy, cz)
	}

	private companion object {
		/**
		 * 在给定 SAT 分离轴上测试凸多边形投影与 AABB 投影是否重叠。
		 *
		 * @param worldVerts  世界坐标下的凸多边形顶点
		 * @param aabbCenter  AABB 中心
		 * @param aabbHalf    AABB 半边长
		 * @param axis        分离轴
		 * @return true 表示在该轴上两投影重叠
		 */
		fun testAxis(
			worldVerts: List<Vector3f>, aabbCenter: Vector3f, aabbHalf: Vector3f, axis: Vector3f,
		): Boolean {
			val hullMin = worldVerts.minOf { it.dot(axis) }
			val hullMax = worldVerts.maxOf { it.dot(axis) }
			val aabbProjCenter = aabbCenter.dot(axis)
			val aabbProjHalf = abs(axis.x) * aabbHalf.x + abs(axis.y) * aabbHalf.y + abs(axis.z) * aabbHalf.z
			val aabbMin = aabbProjCenter - aabbProjHalf
			val aabbMax = aabbProjCenter + aabbProjHalf
			return !(hullMin > aabbMax || hullMax < aabbMin)
		}

		/**
		 * 从凸多边形的顶点列表中计算面法线。
		 *
		 * 遍历所有三点组合，计算候选法线，
		 * 仅保留所有顶点在法线同侧（即外表面）的法线。
		 * 对近似平行的法线进行去重。
		 *
		 * @param verts 顶点列表
		 * @return 唯一的外表面法线列表
		 */
		fun computeFaceNormals(verts: List<Vector3f>): List<Vector3f> {
			val normals = mutableListOf<Vector3f>()
			val n = verts.size
			if (n < 3) return normals
			for (i in 0 until n) {
				for (j in i + 1 until n) {
					for (k in j + 1 until n) {
						val e1 = Vector3f(verts[j]).sub(verts[i])
						val e2 = Vector3f(verts[k]).sub(verts[i])
						val normal = Vector3f(e1).cross(e2)
						if (normal.length() < 1e-6f) continue
						normal.normalize()

						var allSameSide = true
						var firstSide = true
						var sideDetermined = false
						for (m in 0 until n) {
							if (m == i || m == j || m == k) continue
							val dot = normal.dot(Vector3f(verts[m]).sub(verts[i]))
							if (!sideDetermined) {
								firstSide = dot >= 0
								sideDetermined = true
							} else if ((dot >= 0) != firstSide && abs(dot) > 1e-6f) {
								allSameSide = false
								break
							}
						}
						if (allSameSide) {
							val existing = normals.any { abs(it.dot(normal)) > 0.99f }
							if (!existing) normals.add(normal)
						}
					}
				}
			}
			return normals
		}
	}
}
