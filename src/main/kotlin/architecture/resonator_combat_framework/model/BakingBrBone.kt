package architecture.resonator_combat_framework.model

import architecture.resonator_combat_framework.util.RcfUtil
import com.google.gson.JsonElement
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * 烘培骨骼数据——运行时使用的简化骨骼结构。
 *
 * @property name 骨骼名称
 * @property parent 父骨骼名称
 * @property pivot 轴心点（模型空间坐标）
 * @property rotation 默认旋转（欧拉角，度）
 * @property cubes 立方体列表
 * @property locators 定位器映射
 */
data class BakingBrBone
@JvmOverloads
constructor(
	val name: String,
	val parent: String? = null,
	val pivot: Vector3fc = Vector3f(0f, 0f, 0f),
	val rotation: Vector3fc = Vector3f(0f, 0f, 0f),
	val cubes: List<BakingBrCube> = emptyList(),
	val locators: Map<String, BakingBrLocator> = emptyMap()
) {
	companion object {
		/**
		 * 从 JSON 数组解析 [BakingBrBone] 列表。
		 *
		 * @param json JSON 元素
		 * @return 骨骼名称到烘培骨骼的映射
		 */
		@JvmStatic
		fun parses(json: JsonElement): Map<String, BakingBrBone> {
			val list = mutableMapOf<String, BakingBrBone>()
			json.asJsonArray.forEach { jsonElement ->
				try {
					val obj = jsonElement.asJsonObject
					val name = obj.get("name").asString
					val parent = obj.get("parent")?.asString
					val pivot = obj.getAsJsonArray("pivot")?.map { it.asFloat } ?: listOf(0f, 0f, 0f)
					val rotation = obj.getAsJsonArray("rotation")?.map { it.asFloat } ?: listOf(0f, 0f, 0f)
					val cubes = obj.get("cubes")?.run { BakingBrCube.parses(this) } ?: emptyList()

					val locators = BakingBrLocator.parses(name, obj)
					list[name] = BakingBrBone(
						name,
						parent,
						Vector3f(pivot[0], pivot[1], pivot[2]),
						Vector3f(rotation[0], rotation[1], rotation[2]),
						cubes,
						locators
					)
				} catch (e: Exception) {
					RcfUtil.LOGGER.warn("[MODEL] Failed to parse bone: {} - element: {}", e.message, jsonElement)
				}
			}
			return list.toMap()
		}
	}
}