package architecture.resonator_combat_framework.module.collision

import architecture.resonator_combat_framework.module.collision.AnvilLibCollision.intersectsAABBTriangle
import architecture.resonator_combat_framework.module.collision.AnvilLibCollision.updateInterval
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3fc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 来源
 * [AnvilLib](https://github.com/Anvil-Dev/AnvilLib/blob/82b4f6afd6e7009f35bdc8080bbf01a7e526d875/module.collision/src/main/java/dev/anvilcraft/lib/v2/collision/AnvilLibCollision.java)
 */
object AnvilLibCollision {

	/**
	 * 沿三轴独立扫掠：AABB 分别沿 X、Y、Z 轴运动，逐轴计算最大安全位移。
	 *
	 * 每个轴的处理是独立的——沿 X 扫掠时 Y、Z 坐标保持不变，沿 Y 扫掠时 X、Z
	 * 不变。因此当对角方向有障碍物时可能偏保守。
	 *
	 * 三轴独立的好处是与 Minecraft 原版碰撞一致：各分量被各自的障碍物"推开"，
	 * 避免了沿斜面滑动的问题。
	 *
	 * @param min       AABB 最小角（世界坐标）
	 * @param max       AABB 最大角（世界坐标）
	 * @param motion    各轴最大位移（分量可正可负）
	 * @param triangles 三角形数组，每 3 个顶点构成一个三角形
	 * @param epsilon   碰撞容差——间距小于此值即视为碰撞
	 * @return 实际可移动位移，各分量符号与 [motion] 一致，
	 * 绝对值 ≤ [motion] 绝对值。已重叠 → 0，无碰撞 → 原始分量
	 */
	@JvmStatic
	fun intersectsAABBTriangle(
		min: Vector3dc,
		max: Vector3dc,
		motion: Vector3dc,
		triangles: Array<Vector3fc>,
		epsilon: Double
	): Vector3dc {
		val center = min.add(max, Vector3d()).mul(0.5)
		val halfExtents = max.sub(min, Vector3d()).mul(0.5)
		val result = Vector3d()

		for (axisIndex in 0..2) {
			val d = motion.get(axisIndex)
			if (abs(d) < 1e-15) {
				result.setComponent(axisIndex, 0.0)
				continue
			}

			val axisVec = Vector3d().also { it.setComponent(axisIndex, 1.0) }

			var safeDist = d

			val triCount = triangles.size / 3
			for (i in 0 until triCount) {
				val v0 = triangles[i * 3]
				val v1 = triangles[i * 3 + 1]
				val v2 = triangles[i * 3 + 2]

				val tHit = collisionDistanceOnAxis(v0, v1, v2, center, halfExtents, axisVec, d, epsilon)

				if (d > 0) {
					if (tHit in 0.0..<safeDist) {
						safeDist = tHit
					}
				} else {
					if (tHit > safeDist && tHit <= 0) {
						safeDist = tHit
					}
				}

				if ((d > 0 && safeDist <= 0) || (d < 0 && safeDist >= 0)) {
					break
				}
			}
			result.setComponent(axisIndex, safeDist)
		}
		return result
	}

	/**
	 * 真扫掠碰撞：AABB 沿 [motion] 方向平移，求首次碰到任意三角形时的位移。
	 *
	 * 与 [intersectsAABBTriangle] 的三轴独立不同，此方法沿 motion 的合成方向扫掠，
	 * 适用于需要精确斜角碰撞判定的场景。
	 *
	 * 返回值为 `motion * t`，其中 t ∈ [0,1] 为安全比例：
	 *
	 *   - t = 1 → 全程无碰撞
	 *   - t = 0 → 起始位置已重叠
	 *   - 0 < t < 1 → 碰撞发生在运动途中
	 *
	 * 对每个三角形使用 SAT（分离轴定理）计算碰撞时间区间，取所有三角形中的最小值。
	 *
	 * @param min       AABB 最小角（世界坐标）
	 * @param max       AABB 最大角（世界坐标）
	 * @param motion    位移向量（方向 + 大小）
	 * @param triangles 三角形数组，每 3 个顶点构成一个三角形
	 * @param epsilon   碰撞容差——间距小于此值即视为碰撞
	 * @return 沿 [motion] 方向的最大安全位移向量。
	 * 无碰撞时等于 [motion]，初始重叠时为零向量
	 */
	@JvmStatic
	fun sweptCollisionAABBTriangle(
		min: Vector3dc,
		max: Vector3dc,
		motion: Vector3dc,
		triangles: Array<Vector3fc>,
		epsilon: Double
	): Vector3dc {
		val dist = motion.length()
		if (dist < 1e-15) {
			return Vector3d()
		}

		val dir = motion.div(dist, Vector3d())
		val center = min.add(max, Vector3d()).mul(0.5)
		val halfExtents = max.sub(min, Vector3d()).mul(0.5)

		var minT = dist
		val triCount = triangles.size / 3
		for (i in 0 until triCount) {
			val v0 = triangles[i * 3]
			val v1 = triangles[i * 3 + 1]
			val v2 = triangles[i * 3 + 2]

			val tHit = collisionDistanceOnAxis(v0, v1, v2, center, halfExtents, dir, dist, epsilon)
			if (tHit < minT) {
				minT = tHit
				if (minT <= 0) break
			}
		}

		return dir.mul(minT, Vector3d())
	}

	/**
	 * 计算 AABB 沿 [axisVec] 方向运动时，与单个三角形的首次碰撞位移。
	 *
	 * 三角形顶点被平移到以 [center] 为原点的局部坐标系，
	 * 使用 SAT（分离轴定理）扫描 13 条轴（3 坐标轴 + 1 面法线 + 9 边叉积轴），
	 * 由 [updateInterval] 逐轴收窄碰撞时间区间，最后取与运动方向一致的边界。
	 *
	 * @param v0,v1,v2    三角形顶点（世界坐标）
	 * @param center      AABB 中心点
	 * @param halfExtents AABB 半边长
	 * @param axisVec     运动方向（无需单位化）
	 * @param d           最大位移量（沿 [axisVec] 方向，可正可负）
	 * @param epsilon     碰撞容差
	 * @return 首次碰撞位移（有符号）。0 = 初始已重叠，d = 全程无碰撞
	 */
	private fun collisionDistanceOnAxis(
		v0: Vector3fc,
		v1: Vector3fc,
		v2: Vector3fc,
		center: Vector3dc,
		halfExtents: Vector3dc,
		axisVec: Vector3dc,
		d: Double,
		epsilon: Double
	): Double {
		// 将三角形平移到 AABB 中心为原点的局部坐标
		val v0l = Vector3d(v0).sub(center)
		val v1l = Vector3d(v1).sub(center)
		val v2l = Vector3d(v2).sub(center)

		// 初始就已经碰撞 → 安全距离为 0
		if (initialOverlap(v0l, v1l, v2l, halfExtents, epsilon)) {
			return 0.0
		}

		// 三角形边向量
		val f0 = v1l.sub(v0l, Vector3d())
		val f1 = v2l.sub(v1l, Vector3d())
		val f2 = v0l.sub(v2l, Vector3d())

		// 面法线
		val normal = f0.cross(f1, Vector3d())
		val hasNormal = normal.lengthSquared() > 1e-15
		if (hasNormal) normal.normalize()

		// AABB 三个面法线
		val aabbAxes = arrayOf(
			Vector3d(1.0, 0.0, 0.0),
			Vector3d(0.0, 1.0, 0.0),
			Vector3d(0.0, 0.0, 1.0)
		)
		val triEdges = arrayOf(f0, f1, f2)

		// 交集区间 [tLow, tHigh] 表示所有分离轴都重叠的 t 范围
		val interval = doubleArrayOf(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)

		// 测试 3 个坐标轴
		for (axis in aabbAxes) {
			if (!updateInterval(axis, v0l, v1l, v2l, halfExtents, axisVec, epsilon, interval)) {
				return d // 无碰撞
			}
		}

		// 测试三角形面法线
		if (hasNormal) {
			if (!updateInterval(normal, v0l, v1l, v2l, halfExtents, axisVec, epsilon, interval)) {
				return d
			}
		}

		// 测试 9 条边叉积轴
		for (edge in triEdges) {
			for (aabbAxis in aabbAxes) {
				val axis = edge.cross(aabbAxis, Vector3d())
				if (axis.lengthSquared() > 1e-15) {
					axis.normalize()
					if (!updateInterval(axis, v0l, v1l, v2l, halfExtents, axisVec, epsilon, interval)) {
						return d
					}
				}
			}
		}

		val tLow = interval[0]
		val tHigh = interval[1]

		// 若交集为空或退化，则无碰撞
		if (tLow > tHigh + 1e-12) {
			return d
		}

		// 根据运动方向取第一个（最接近 0 的）碰撞位移
		if (d > 0) {
			if (tHigh < 0) return d   // 区间全在负半轴
			if (tLow > 0) return tLow
		} else {
			if (tLow > 0) return d    // 区间全在正半轴
			if (tHigh < 0) return tHigh
		}
		return d
	}

	/**
	 * 在给定 SAT 分离轴上计算 AABB 与三角形重叠的 t 区间，并与当前区间求交。
	 *
	 * AABB 中心沿 [axisVec] 运动，在参数 t 时刻的位置为 `center + t * axisVec`。
	 * 三角形在轴上的投影为 [tMin, tMax]，AABB 的投影半径在运动中不变（仅平移，不旋转）。
	 *
	 * 若该轴与运动方向垂直（`dot ≈ 0`），则检查静态分离；否则求解线性不等式
	 * 得到重叠的 t 范围 [low, high]，与当前区间取交集。
	 *
	 * @param axis        SAT 分离轴（无需单位化）
	 * @param v0,v1,v2    局部坐标系下的三角形顶点
	 * @param halfExtents AABB 半边长
	 * @param axisVec     运动方向
	 * @param epsilon     碰撞容差
	 * @param interval    [0]=tLow, [1]=tHigh，会被就地更新
	 * @return false 表示该轴永久分离，运动全程不可能碰撞
	 */
	private fun updateInterval(
		axis: Vector3d,
		v0: Vector3d,
		v1: Vector3d,
		v2: Vector3d,
		halfExtents: Vector3dc,
		axisVec: Vector3dc,
		epsilon: Double,
		interval: DoubleArray
	): Boolean {
		// 三角形投影
		val p0 = axis.dot(v0)
		val p1 = axis.dot(v1)
		val p2 = axis.dot(v2)
		val tMin = min(min(p0, p1), p2)
		val tMax = max(max(p0, p1), p2)

		// AABB 投影半径
		val r = abs(axis.x()) * halfExtents.x() + abs(axis.y()) * halfExtents.y() + abs(axis.z()) * halfExtents.z()

		val dot = axis.dot(axisVec) // 移动方向在分离轴上的投影

		if (abs(dot) < 1e-15) {
			// 轴与运动方向垂直 → 投影不随 t 改变
			// 若此时该轴是分离轴，则运动全程分离 → 无碰撞可能
			return !(-r > tMax + epsilon) && !(tMin - epsilon > r)
			// 否则该轴始终重叠，不提供约束
		}

		val low: Double
		val high: Double
		if (dot > 0) {
			low = (tMin - epsilon - r) / dot
			high = (tMax + epsilon + r) / dot
		} else {
			low = (tMax + epsilon + r) / dot // dot 为负，low 是较小值
			high = (tMin - epsilon - r) / dot
		}

		interval[0] = max(interval[0], low)
		interval[1] = min(interval[1], high)

		return interval[0] <= interval[1] + 1e-12
	}

	/**
	 * 静态 SAT 检查：AABB 与三角形在初始位置（t=0，局部坐标）是否重叠。
	 *
	 * 测试全部 13 条分离轴（3 坐标轴 + 1 面法线 + 9 边叉积轴），
	 * 若任意轴分离则返回 false。
	 *
	 * @param v0,v1,v2    局部坐标系下的三角形顶点（已减去 AABB 中心）
	 * @param halfExtents AABB 半边长
	 * @param epsilon     碰撞容差
	 * @return true 表示重叠（或 gap ≤ epsilon）
	 */
	private fun initialOverlap(
		v0: Vector3d,
		v1: Vector3d,
		v2: Vector3d,
		halfExtents: Vector3dc,
		epsilon: Double
	): Boolean {
		// 三个坐标轴
		if (!overlapOnAxis(Vector3d(1.0, 0.0, 0.0), v0, v1, v2, halfExtents, epsilon)) return false
		if (!overlapOnAxis(Vector3d(0.0, 1.0, 0.0), v0, v1, v2, halfExtents, epsilon)) return false
		if (!overlapOnAxis(Vector3d(0.0, 0.0, 1.0), v0, v1, v2, halfExtents, epsilon)) return false

		val f0 = v1.sub(v0, Vector3d())
		val f1 = v2.sub(v1, Vector3d())
		val normal = f0.cross(f1, Vector3d())
		if (normal.lengthSquared() > 1e-15) {
			normal.normalize()
			if (!overlapOnAxis(normal, v0, v1, v2, halfExtents, epsilon)) return false
		}

		val f2 = v0.sub(v2, Vector3d())
		val edges = arrayOf(f0, f1, f2)
		val axes = arrayOf(
			Vector3d(1.0, 0.0, 0.0),
			Vector3d(0.0, 1.0, 0.0),
			Vector3d(0.0, 0.0, 1.0)
		)
		for (edge in edges) {
			for (a in axes) {
				val axis = edge.cross(a, Vector3d())
				if (axis.lengthSquared() > 1e-15) {
					axis.normalize()
					if (!overlapOnAxis(axis, v0, v1, v2, halfExtents, epsilon)) return false
				}
			}
		}
		return true // 所有轴都重叠 → 碰撞
	}

	/**
	 * 静态碰撞检测：判断 AABB 与任意三角形是否重叠。
	 *
	 * 对每个三角形先做包围盒粗筛（AABB-AABB 快速剔除），
	 * 再以完整的 SAT（13 条分离轴）进行精确判定。
	 *
	 * @param min       AABB 最小角
	 * @param max       AABB 最大角
	 * @param triangles 三角形数组，每 3 个顶点构成一个三角形
	 * @param epsilon   碰撞容差——间距小于此值即视为重叠
	 * @return true 表示存在至少一个三角形与 AABB 碰撞
	 */
	@JvmStatic
	fun intersectsAABBTriangle(
		min: Vector3dc,
		max: Vector3dc,
		triangles: Array<Vector3fc>,
		epsilon: Double
	): Boolean {
		val center = min.add(max, Vector3d()).mul(0.5)
		val halfExtents = max.sub(min, Vector3d()).mul(0.5)

		val triangleCount = triangles.size / 3
		for (i in 0 until triangleCount) {
			val v0 = triangles[i * 3]
			val v1 = triangles[i * 3 + 1]
			val v2 = triangles[i * 3 + 2]

			if (triangleIntersectsAABB(v0, v1, v2, center, halfExtents, epsilon)) {
				return true
			}
		}
		return false
	}

	/**
	 * 对单个三角形执行完整 SAT 碰撞检测（13 条轴）。
	 *
	 * 先做三角形包围盒粗筛（AABB-AABB），再将顶点平移到以 AABB 中心为原点的
	 * 局部坐标系，依次测试 3 坐标轴 → 面法线 → 9 条边叉积轴。
	 *
	 * @param v0,v1,v2    三角形顶点（世界坐标）
	 * @param center      AABB 中心点
	 * @param halfExtents AABB 半边长
	 * @param epsilon     碰撞容差
	 * @return true 表示三角形与 AABB 重叠（或 gap ≤ epsilon）
	 */
	private fun triangleIntersectsAABB(
		v0: Vector3fc,
		v1: Vector3fc,
		v2: Vector3fc,
		center: Vector3dc,
		halfExtents: Vector3dc,
		epsilon: Double
	): Boolean {
		// 1. 三角形包围盒粗筛（快速剔除）
		val triMinX = min(min(v0.x(), v1.x()).toDouble(), v2.x().toDouble())
		val triMinY = min(min(v0.y(), v1.y()).toDouble(), v2.y().toDouble())
		val triMinZ = min(min(v0.z(), v1.z()).toDouble(), v2.z().toDouble())
		val triMaxX = max(max(v0.x(), v1.x()).toDouble(), v2.x().toDouble())
		val triMaxY = max(max(v0.y(), v1.y()).toDouble(), v2.y().toDouble())
		val triMaxZ = max(max(v0.z(), v1.z()).toDouble(), v2.z().toDouble())

		val boxMinX = center.x() - halfExtents.x()
		val boxMinY = center.y() - halfExtents.y()
		val boxMinZ = center.z() - halfExtents.z()
		val boxMaxX = center.x() + halfExtents.x()
		val boxMaxY = center.y() + halfExtents.y()
		val boxMaxZ = center.z() + halfExtents.z()

		if (triMinX > boxMaxX + epsilon || triMaxX < boxMinX - epsilon ||
			triMinY > boxMaxY + epsilon || triMaxY < boxMinY - epsilon ||
			triMinZ > boxMaxZ + epsilon || triMaxZ < boxMinZ - epsilon
		) {
			return false
		}

		// 2. 将三角形顶点平移到 AABB 中心为原点的局部坐标（使用 double 保证精度）
		val v0l = Vector3d(v0).sub(center)
		val v1l = Vector3d(v1).sub(center)
		val v2l = Vector3d(v2).sub(center)

		// 3. 三角形边向量
		val f0 = v1l.sub(v0l, Vector3d())
		val f1 = v2l.sub(v1l, Vector3d())
		val f2 = v0l.sub(v2l, Vector3d())

		val aabbAxisX = Vector3d(1.0, 0.0, 0.0)
		val aabbAxisY = Vector3d(0.0, 1.0, 0.0)
		val aabbAxisZ = Vector3d(0.0, 0.0, 1.0)

		// ---- 测试三个坐标轴 ----
		if (!overlapOnAxis(aabbAxisX, v0l, v1l, v2l, halfExtents, epsilon)) return false
		if (!overlapOnAxis(aabbAxisY, v0l, v1l, v2l, halfExtents, epsilon)) return false
		if (!overlapOnAxis(aabbAxisZ, v0l, v1l, v2l, halfExtents, epsilon)) return false

		// ---- 测试三角形面法线轴 ----
		val normal = f0.cross(f1, Vector3d())
		if (normal.lengthSquared() > 1e-15) {
			normal.normalize()
			if (!overlapOnAxis(normal, v0l, v1l, v2l, halfExtents, epsilon)) return false
		}
		// 如果面积极小（退化为线段或点），跳过此轴（后续边叉积轴可能仍有效）

		// ---- 测试 9 条边叉积轴 ----
		val aabbAxes = arrayOf(aabbAxisX, aabbAxisY, aabbAxisZ)
		val triEdges = arrayOf(f0, f1, f2)

		for (edge in triEdges) {
			for (aabbAxis in aabbAxes) {
				val axis = edge.cross(aabbAxis, Vector3d())
				if (axis.lengthSquared() > 1e-15) {
					axis.normalize()
					if (!overlapOnAxis(axis, v0l, v1l, v2l, halfExtents, epsilon)) {
						return false
					}
				}
				// 平行或退化轴忽略，不会提供有效分离
			}
		}

		// 所有轴都重叠 ⇒ 碰撞
		return true
	}

	/**
	 * 测试三角形与中心在原点的 AABB 在给定轴上的投影是否重叠。
	 *
	 * 三角形投影区间 [tMin, tMax]，AABB 投影区间 [−r, r]，其中
	 * r = Σ|axis[i] · halfExtents[i]|。若两区间带容差后仍分离则返回 false。
	 *
	 * 分离条件：tMin > r + epsilon 或 −r > tMax + epsilon。
	 *
	 * @param axis           投影轴（无需单位化）
	 * @param v0,v1,v2       局部坐标系下的三角形顶点
	 * @param boxHalfExtents AABB 半边长
	 * @param epsilon        碰撞容差——扩大 AABB 投影半径使"接近"也视为重叠
	 * @return true 表示两投影区间有交集
	 */
	@JvmStatic
	fun overlapOnAxis(
		axis: Vector3dc,
		v0: Vector3dc,
		v1: Vector3dc,
		v2: Vector3dc,
		boxHalfExtents: Vector3dc,
		epsilon: Double
	): Boolean {
		val p0 = axis.x() * v0.x() + axis.y() * v0.y() + axis.z() * v0.z()
		val p1 = axis.x() * v1.x() + axis.y() * v1.y() + axis.z() * v1.z()
		val p2 = axis.x() * v2.x() + axis.y() * v2.y() + axis.z() * v2.z()

		val tMin = min(min(p0, p1), p2)
		val tMax = max(max(p0, p1), p2)

		val r = abs(axis.x() * boxHalfExtents.x()) +
			abs(axis.y() * boxHalfExtents.y()) +
			abs(axis.z() * boxHalfExtents.z())

		return !(tMin > r + epsilon) && !(-r > tMax + epsilon)
	}
}
