package architecture.resonator_combat_framework.module.entity_animation.animation.model

import architecture.resonator_combat_framework.util.RcfUtil
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * 几何模型数据——存储从 JSON 解析的实体几何结构。
 *
 * 包含骨骼、立方体和定位器的定义，是运行时模型装配的源数据。
 *
 * @property identifier 模型标识符
 * @property bones 骨骼名称到烘培骨骼数据的映射
 * @property locators 定位器名称到烘培定位器数据的映射
 */
data class GeometryData
@JvmOverloads
constructor(
	val identifier: String,
	val bones: Map<String, BakingBrBone> = emptyMap(),
	val locators: Map<String, BakingBrLocator> = emptyMap()
) {
	companion object {

		@JvmField
		val EMPTY = GeometryData("empty")

		/**
		 * 从 JSON 元素解析 [GeometryData] 列表。
		 * 兼容 Minecraft 基岩版几何模型格式（minecraft:geometry 数组）。
		 *
		 * @param json JSON 元素
		 * @return 解析后的几何模型数据列表
		 */
		@JvmStatic
		fun parses(json: JsonElement): List<GeometryData> {
			val root = json.asJsonObject
			val result = mutableListOf<GeometryData>()
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

				result.add(GeometryData(identifier, bones, locators))
			}
			return result
		}
	}
}

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

/**
 * 烘培立方体数据——模型中的单个立方体定义。
 *
 * @property inflate 膨胀值
 * @property origin 原点坐标
 * @property size 尺寸
 * @property rotation 旋转
 */
data class BakingBrCube
@JvmOverloads
constructor(
	val inflate: Float = 0f,
	val origin: Vector3fc = Vector3f(),
	val size: Vector3fc = Vector3f(),
	val rotation: Vector3fc = Vector3f()
) {
	companion object {
		/**
		 * 从 JSON 数组解析 [BakingBrCube] 列表。
		 *
		 * @param json JSON 元素
		 * @return 立方体列表
		 */
		@JvmStatic
		fun parses(json: JsonElement): List<BakingBrCube> {
			val list = mutableListOf<BakingBrCube>()
			json.asJsonArray.forEach {
				val obj = it.asJsonObject
				val origin = obj.getAsJsonArray("origin")?.map { it1 -> it1.asFloat } ?: listOf(0f, 0f, 0f)
				val size = obj.getAsJsonArray("size")?.map { it1 -> it1.asFloat } ?: listOf(0f, 0f, 0f)
				val inflate = obj.get("inflate")?.asFloat ?: 0f
				val rotation = obj.getAsJsonArray("size")?.map { it1 -> it1.asFloat } ?: listOf(0f, 0f, 0f)
				list.add(
					BakingBrCube(
						inflate,
						Vector3f(origin[0], origin[1], origin[2]),
						Vector3f(size[0], size[1], size[2]),
						Vector3f(rotation[0], rotation[1], rotation[2]),
					)
				)
			}
			return list
		}
	}
}

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
