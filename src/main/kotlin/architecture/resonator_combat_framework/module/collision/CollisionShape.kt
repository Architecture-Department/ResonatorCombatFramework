package architecture.resonator_combat_framework.module.collision

import architecture.goldenboughs_lib.api.AllOpe
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 碰撞形状 —— sealed interface，所有碰撞体类型都实现此接口。
 *
 * 坐标约定：
 * - 若 [boneName] 不为 null，center/halfExtents 为**骨骼局部坐标**
 * - 若 [boneName] 为 null，center/halfExtents 为**实体局部坐标**（相对实体原点）
 *
 * 外部系统（如攻击动画）只需关注相对坐标，世界变换由 [CollisionSystem] 在 tick 时处理。
 */
@AllOpe
interface CollisionShape {
	/**
	 * 检测此碰撞体是否与目标实体的 AABB 相交。
	 */
	fun checkCollision(entry: CollisionEntry, attacker: Entity, targetBox: AABB): Boolean

	/**
	 * 计算此碰撞体在世界空间中的包围球半径和中心位置，用于构建搜索 AABB。
	 */
	fun computeWorldBounds(entry: CollisionEntry, attacker: Entity): WorldBounds

	/**
	 * OBB（有向包围盒），可绑定到骨骼。
	 *
	 * @param boneName   绑定的骨骼名。不为 null 时，需要对应的 boneMatrix 才能变换到世界空间
	 * @param center     碰撞体中心相对骨骼原点（或实体原点）的偏移
	 * @param halfExtents 碰撞体半边长
	 */
	data class OBB(
		val boneName: String? = null,
		val center: Vector3f,
		val halfExtents: Vector3f,
	) : CollisionShape {

		override fun checkCollision(entry: CollisionEntry, attacker: Entity, targetBox: AABB): Boolean {
			val h = halfExtents
			val sphereRadius = sqrt(h.x * h.x + h.y * h.y + h.z * h.z)

			val shapeCenter = if (entry.worldMatrix != null) {
				Vector3f(entry.worldMatrix.m30(), entry.worldMatrix.m31(), entry.worldMatrix.m32())
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
				return AnimCollider.obbAabbOverlap(
					entry.worldMatrix, AnimCollider(center, halfExtents), targetBox
				)
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
	}

	/**
	 * ConvexHull（凸多边形碰撞体），可绑定到骨骼。
	 *
	 * @param boneName  绑定的骨骼名。不为 null 时，需要对应的 boneMatrix 才能变换到世界空间
	 * @param vertices  定义凸多边形的顶点列表（模型坐标，需构成凸多面体）
	 */
	data class ConvexHull(
		val boneName: String? = null,
		val vertices: List<Vector3f>,
	) : CollisionShape {

		override fun checkCollision(entry: CollisionEntry, attacker: Entity, targetBox: AABB): Boolean {
			// 变换顶点到世界空间
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

			// SAT——AABB轴
			if (!testAxis(worldVerts, aabbCenter, aabbHalf, Vector3f(1f, 0f, 0f))) return false
			if (!testAxis(worldVerts, aabbCenter, aabbHalf, Vector3f(0f, 1f, 0f))) return false
			if (!testAxis(worldVerts, aabbCenter, aabbHalf, Vector3f(0f, 0f, 1f))) return false

			// SAT——凸多边形面法线
			val faceNormals = computeFaceNormals(worldVerts)
			for (normal in faceNormals) {
				if (!testAxis(worldVerts, aabbCenter, aabbHalf, normal)) return false
			}

			// SAT——边叉积 × AABB轴
			for (i in 0 until worldVerts.size) {
				for (j in i + 1 until worldVerts.size) {
					val edge = Vector3f(worldVerts[j]).sub(worldVerts[i])
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
			/** 在指定轴上测试凸多边形与 AABB 是否分离 */
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

			/** 从顶点列表计算凸多边形的面法线（假设顶点已变换到世界空间） */
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

							// 检查所有顶点是否在平面同一侧
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
								// 去重（相同方向的面法线）
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

}

/**
 * 碰撞体的世界空间包围球信息，用于构建搜索 AABB。
 */
data class WorldBounds(
	val sphereRadius: Float,
	val cx: Double,
	val cy: Double,
	val cz: Double,
)
