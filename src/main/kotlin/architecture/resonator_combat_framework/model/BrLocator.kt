package architecture.resonator_combat_framework.model

import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * 运行时定位器数据。
 *
 * @property name 定位器名称
 * @property boneName 所属骨骼名称
 * @property offset 相对骨骼偏移
 * @property rotation 相对骨骼旋转
 */
data class BrLocator
@JvmOverloads
constructor(
	val name: String,
	val boneName: String,
	val offset: Vector3fc = Vector3f(),
	val rotation: Vector3fc = Vector3f()
) {
	companion object {
		/**
		 * 从 [BakingBrLocator] 转换为运行时 [BrLocator]。
		 *
		 * @param locator 烘培定位器数据
		 * @return 运行时定位器实例
		 */
		@JvmStatic
		fun of(locator: BakingBrLocator): BrLocator {
			return BrLocator(
				locator.name,
				locator.boneName,
				Vector3f(locator.offset),
				Vector3f(locator.rotation)
			)
		}
	}
}