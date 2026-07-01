package architecture.resonator_combat_framework.util

import org.joml.Vector3f

/**
 * 旋转插值工具 —— 保证 Euler 角度走最短路径。
 *
 * 直接对 Euler 角度做线性插值会导致例如 350°→10° 绕 340° 而非 20° 的问题，
 * 此函数在插值前修正目标值使各轴差值始终在 [-180°, 180°] 范围内。
 */
object RotationUtil {

	/**
	 * 将 [from] 和 [to] 之间的旋转进行最短路径线性插值，结果写入 [dest]。
	 *
	 * @param from  起始旋转（度）
	 * @param to    目标旋转（度）
	 * @param t     插值因子 [0, 1]
	 * @param dest  输出
	 */
	@JvmStatic
	fun lerpRotation(from: Vector3f, to: Vector3f, t: Float, dest: Vector3f) {
		val dx = to.x - from.x
		val dy = to.y - from.y
		val dz = to.z - from.z

		dest.x = from.x + normalizeDelta(dx) * t
		dest.y = from.y + normalizeDelta(dy) * t
		dest.z = from.z + normalizeDelta(dz) * t
	}

	/**
	 * 将差值归一化到 [-180, 180] 范围，确保走最短路径。
	 */
	/** 将角度差值归一化到 [-180, 180] 范围 */
	@JvmStatic
	fun normalizeDelta(delta: Float): Float {
		val d = delta % 360f
		return if (d > 180f) d - 360f else if (d < -180f) d + 360f else d
	}
}
