package architecture.resonator_combat_framework.module.collision

import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 有向包围盒碰撞器。
 *
 * 对应 [Epic Fight OBBCollider](https://github.com/Antikythera-Studios/epicfight/blob/1.21.1/src/main/java/yesman/epicfight/api/collider/OBBCollider.java)。
 *
 * 使用 6 轴 SAT（分离轴定理）进行碰撞检测：
 * 3 条自身法线轴 + 3 条目标法线轴。
 * 注意 Epic Fight 有意跳过了 9 条边-边叉积轴以提升性能。
 *
 * @param vertexX X 轴半长
 * @param vertexY Y 轴半长
 * @param vertexZ Z 轴半长
 * @param centerX 模型空间 X 偏移
 * @param centerY 模型空间 Y 偏移
 * @param centerZ 模型空间 Z 偏移
 */
class OBBCollider(
	vertexX: Double, vertexY: Double, vertexZ: Double,
	centerX: Double, centerY: Double, centerZ: Double,
) : Collider(
	modelCenter = Vector3f(centerX.toFloat(), centerY.toFloat(), centerZ.toFloat()),
	outerAABB = computeInitialAABB(vertexX, vertexY, vertexZ, centerX, centerY, centerZ),
) {
	/** 模型空间顶点（相对于中心，仅存顶部 4 个角） */
	val modelVertices: Array<Vector3f> = arrayOf(
		Vector3f(vertexX.toFloat(), vertexY.toFloat(), (-vertexZ).toFloat()),
		Vector3f(vertexX.toFloat(), vertexY.toFloat(), vertexZ.toFloat()),
		Vector3f((-vertexX).toFloat(), vertexY.toFloat(), vertexZ.toFloat()),
		Vector3f((-vertexX).toFloat(), vertexY.toFloat(), (-vertexZ).toFloat()),
	)

	/** 模型空间法线（3 个轴方向） */
	val modelNormals: Array<Vector3f> = arrayOf(
		Vector3f(1f, 0f, 0f),
		Vector3f(0f, 1f, 0f),
		Vector3f(0f, 0f, 1f),
	)

	/** 变换后的世界空间顶点 */
	val rotatedVertices: Array<Vector3f> = Array(4) { Vector3f() }

	/** 变换后的世界空间法线 */
	val rotatedNormals: Array<Vector3f> = Array(3) { Vector3f() }

	/** 变换矩阵的缩放系数 */
	val scale: Vector3f = Vector3f(1f)

	/**
	 * 变换碰撞体。
	 *
	 * 对应 Epic Fight `OBBCollider.transform(OpenMatrix4f)`。
	 * 从矩阵中提取旋转部分（移除平移），旋转所有顶点和法线，
	 * 提取缩放系数，然后更新世界空间中心。
	 */
	override fun transform(matrix: Matrix4fc) {
		val rotMat = Matrix4f(matrix)
		rotMat.m30(0f); rotMat.m31(0f); rotMat.m32(0f); rotMat.m33(1f)

		for (i in modelVertices.indices) {
			rotMat.transformPosition(Vector3f(modelVertices[i]), rotatedVertices[i])
		}
		for (i in modelNormals.indices) {
			rotMat.transformDirection(Vector3f(modelNormals[i]), rotatedNormals[i])
		}

		val sx = sqrt(matrix.m00() * matrix.m00() + matrix.m10() * matrix.m10() + matrix.m20() * matrix.m20())
		val sy = sqrt(matrix.m01() * matrix.m01() + matrix.m11() * matrix.m11() + matrix.m21() * matrix.m21())
		val sz = sqrt(matrix.m02() * matrix.m02() + matrix.m12() * matrix.m12() + matrix.m22() * matrix.m22())
		scale.set(sx, sy, sz)

		super.transform(matrix)
	}

	override fun getHitboxAABB(): AABB {
		val dx = (outerAABB!!.maxX - outerAABB.minX) * scale.x() / 2.0
		val dy = (outerAABB.maxY - outerAABB.minY) * scale.y() / 2.0
		val dz = (outerAABB.maxZ - outerAABB.minZ) * scale.z() / 2.0
		return AABB(
			worldCenter.x() - dx, worldCenter.y() - dy, worldCenter.z() - dz,
			worldCenter.x() + dx, worldCenter.y() + dy, worldCenter.z() + dz,
		)
	}

	/**
	 * 6 轴 SAT 碰撞检测：OBB vs OBB。
	 *
	 * 对应 Epic Fight `OBBCollider.isCollide(OBBCollider)`。
	 * 仅测试 6 条法线轴（3 条自身 + 3 条目标），
	 * Epic Fight 因性能原因跳过了 9 条边-边叉积轴。
	 */
	fun isCollide(opponent: OBBCollider): Boolean {
		val toOpponent = Vector3f(opponent.worldCenter).sub(worldCenter)

		for (axis in rotatedNormals) {
			if (!checkAxisOverlap(Vector3f(axis), toOpponent, this, opponent)) return false
		}
		for (axis in opponent.rotatedNormals) {
			if (!checkAxisOverlap(Vector3f(axis), toOpponent, this, opponent)) return false
		}

		return true
	}

	override fun isCollide(target: Entity): Boolean {
		val tempObb = OBBCollider.fromEntityAABB(target.boundingBox)
		return isCollide(tempObb)
	}

	override fun deepCopy(): Collider {
		val v = modelVertices[1]
		return OBBCollider(
			v.x().toDouble(), v.y().toDouble(), v.z().toDouble(),
			modelCenter.x().toDouble(), modelCenter.y().toDouble(), modelCenter.z().toDouble(),
		)
	}

	companion object {
		/**
		 * 从实体 AABB 创建临时 OBB（用于窄相位检测）。
		 * 对应 Epic Fight `OBBCollider(AABB)` 构造器。
		 */
		fun fromEntityAABB(aabb: AABB): OBBCollider {
			val obb = OBBCollider(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
			val xSize = (aabb.maxX - aabb.minX) / 2.0
			val ySize = (aabb.maxY - aabb.minY) / 2.0
			val zSize = (aabb.maxZ - aabb.minZ) / 2.0
			obb.worldCenter.set(
				(aabb.minX + xSize).toFloat(),
				(aabb.minY + ySize).toFloat(),
				(aabb.minZ + zSize).toFloat(),
			)
			obb.rotatedVertices[0] = Vector3f((-xSize).toFloat(), ySize.toFloat(), (-zSize).toFloat())
			obb.rotatedVertices[1] = Vector3f((-xSize).toFloat(), ySize.toFloat(), zSize.toFloat())
			obb.rotatedVertices[2] = Vector3f(xSize.toFloat(), ySize.toFloat(), zSize.toFloat())
			obb.rotatedVertices[3] = Vector3f(xSize.toFloat(), ySize.toFloat(), (-zSize).toFloat())
			obb.rotatedNormals[0] = Vector3f(1f, 0f, 0f)
			obb.rotatedNormals[1] = Vector3f(0f, 1f, 0f)
			obb.rotatedNormals[2] = Vector3f(0f, 0f, 1f)
			return obb
		}

		/**
		 * 计算初始粗包围盒。
		 * 对应 Epic Fight `OBBCollider.getInitialAABB()`。
		 */
		fun computeInitialAABB(
			vx: Double, vy: Double, vz: Double,
			cx: Double, cy: Double, cz: Double,
		): AABB {
			val xLen = abs(vx) + abs(cx)
			val yLen = abs(vy) + abs(cy)
			val zLen = abs(vz) + abs(cz)
			val maxLen = max(xLen, max(yLen, zLen))
			return AABB(maxLen, maxLen, maxLen, -maxLen, -maxLen, -maxLen)
		}
	}
}

/**
 * 检测两个 OBB 在指定分离轴上是否重叠。
 *
 * 对应 Epic Fight `checkSeparateAxisOverlap()`。
 * 将轴翻转指向对手方向，找出两个盒子在该轴上的最大投影范围，
 * 若中心距离 < 投影范围之和则重叠。
 *
 * @param axis 分离轴（副本，不会被修改）
 */
private fun checkAxisOverlap(
	axis: Vector3f, toOpponent: Vector3f,
	box1: OBBCollider, box2: OBBCollider,
): Boolean {
	if (axis.dot(toOpponent) < 0f) axis.mul(-1f)

	var maxProj1 = 0f
	for (v in box1.rotatedVertices) {
		val vv = if (axis.dot(v) > 0f) v else Vector3f(v).mul(-1f)
		maxProj1 = max(maxProj1, axis.dot(vv))
	}

	var maxProj2 = 0f
	for (v in box2.rotatedVertices) {
		val vv = if (axis.dot(v) > 0f) v else Vector3f(v).mul(-1f)
		maxProj2 = max(maxProj2, axis.dot(vv))
	}

	return abs(axis.dot(toOpponent)) < maxProj1 + maxProj2
}
