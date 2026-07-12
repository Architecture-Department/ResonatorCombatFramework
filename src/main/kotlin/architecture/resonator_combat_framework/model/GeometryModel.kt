package architecture.resonator_combat_framework.model

import architecture.resonator_combat_framework.util.RcfUtil
import com.google.gson.JsonElement

/**
 * 几何模型数据——存储从 JSON 解析的实体几何结构。
 *
 * 包含骨骼、立方体和定位器的定义，是运行时模型装配的源数据。
 *
 * @property identifier 模型标识符
 * @property bones 骨骼名称到烘培骨骼数据的映射
 * @property locators 定位器名称到烘培定位器数据的映射
 */
data class GeometryModel
@JvmOverloads
constructor(
	val identifier: String,
	val bones: Map<String, BakingBrBone> = emptyMap(),
	val locators: Map<String, BakingBrLocator> = emptyMap()
) {
	companion object {

		@JvmField
		val EMPTY = GeometryModel("empty")

		/**
		 * 从 JSON 元素解析 [GeometryModel] 列表。
		 * 兼容 Minecraft 基岩版几何模型格式（minecraft:geometry 数组）。
		 *
		 * @param json JSON 元素
		 * @return 解析后的几何模型数据列表
		 */
		@JvmStatic
		fun parses(json: JsonElement): List<GeometryModel> {
			val root = json.asJsonObject
			val result = mutableListOf<GeometryModel>()
			val geometryArray = root.getAsJsonArray("minecraft:geometry") ?: return result
			for (element in geometryArray) {
				val obj = element.asJsonObject
				val desc = obj.getAsJsonObject("description")
				val identifier = desc?.get("identifier")?.asString ?: "unknown"
				val bones = try {
					obj.get("bones")?.run { BakingBrBone.parses(this) } ?: emptyMap()
				} catch (e: Exception) {
					RcfUtil.LOGGER.warn("[MODEL] Failed to parse bones for identifier '{}': {}", identifier, e.message)
					emptyMap()
				}

				// 解析几何级别定位器
				val locators = mutableMapOf<String, BakingBrLocator>()

				// 解析骨骼级别的定位器
				for (bone in bones) {
					for ((name, loc) in bone.value.locators) {
						locators[name] = loc
					}
				}

				result.add(GeometryModel(identifier, bones, locators))
			}
			return result
		}
	}
}