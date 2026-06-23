package architecture.resonator_combat_framework.module.entity_animation.animation.model

import architecture.resonator_combat_framework.util.RcfUtil
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.joml.Vector3f
import org.joml.Vector3fc

data class BakingBrModel
@JvmOverloads
constructor(
	val identifier: String,
	val bones: Map<String, BakingBrBone> = emptyMap(),
	val locators: Map<String, BakingBrLocator> = emptyMap()
) {
	companion object {
		@JvmField
		val EMPTY = BakingBrModel("empty")

		@JvmStatic
		fun parses(json: JsonElement): List<BakingBrModel> {
			val root = json.asJsonObject
			val result = mutableListOf<BakingBrModel>()
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

				result.add(BakingBrModel(identifier, bones, locators))
			}
			return result
		}
	}
}

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

data class BakingBrCube
@JvmOverloads
constructor(
	val inflate: Float = 0f,
	val origin: Vector3fc = Vector3f(),
	val size: Vector3fc = Vector3f(),
	val rotation: Vector3fc = Vector3f()
) {
	companion object {
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

data class BakingBrLocator
@JvmOverloads
constructor(
	val name: String,
	val boneName: String,
	val offset: Vector3fc = Vector3f(),
	val rotation: Vector3fc = Vector3f()
) {

	companion object {
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