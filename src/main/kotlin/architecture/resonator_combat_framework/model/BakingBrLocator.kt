package architecture.resonator_combat_framework.model

import com.google.gson.JsonObject
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * 烘培定位器数据——骨骼上的附着点定义。
 *
 * 用于粒子发射器、音效等特效的定位。
 *
 * @property name 定位器名称
 * @property boneName 所属骨骼名称
 * @property offset 相对骨骼的偏移
 * @property rotation 相对骨骼的旋转
 */
data class BakingBrLocator
@JvmOverloads
constructor(
	val name: String,
	val boneName: String,
	val offset: Vector3fc = Vector3f(),
	val rotation: Vector3fc = Vector3f()
) {

	companion object {
		/**
		 * 从 JSON 对象解析指定骨骼的定位器列表。
		 *
		 * @param boneName 骨骼名称
		 * @param obj 骨骼 JSON 对象
		 * @return 定位器名称到定位器数据的映射
		 */
		@JvmStatic
		fun parses(boneName: String, obj: JsonObject?): MutableMap<String, BakingBrLocator> {
			val locators = mutableMapOf<String, BakingBrLocator>()
			obj?.getAsJsonObject("locators")?.let { locObj ->
				for ((locName, value) in locObj.entrySet()) {
					val offset: Vector3f
					var rotation = Vector3f()
					if (value.isJsonArray) {
						offset = value.asJsonArray?.map { it.asFloat }
							?.run { Vector3f(this[0], this[1], this[2]) }
							?: listOf(0f, 0f, 0f).run { Vector3f(this[0], this[1], this[2]) }
					} else {
						val asJsonObject = value.asJsonObject
						offset = asJsonObject.getAsJsonArray("offset")?.map { it.asFloat }
							?.run { Vector3f(this[0], this[1], this[2]) }
							?: listOf(0f, 0f, 0f).run { Vector3f(this[0], this[1], this[2]) }
						rotation = asJsonObject.getAsJsonArray("position")?.map { it.asFloat }
							?.run { Vector3f(this[0], this[1], this[2]) }
							?: listOf(0f, 0f, 0f).run { Vector3f(this[0], this[1], this[2]) }
					}

					locators[locName] = BakingBrLocator(locName, boneName, offset, rotation)
				}
			}
			return locators
		}
	}
}