package architecture.resonator_combat_framework.module.entity_animation.engine

import com.google.gson.JsonElement
import org.joml.Vector3f

data class BrBone(
	val name: String,
	val parent: String? = null,
	val pivot: Vector3f = Vector3f(0f, 0f, 0f),
	val rotation: Vector3f = Vector3f(0f, 0f, 0f),
	val cubes: List<BrCube> = emptyList(),
	val locators: Map<String, BrLocator> = emptyMap()
) {
	companion object {
		@JvmStatic
		fun parses(json: JsonElement): List<BrBone> {
			val list = mutableListOf<BrBone>()
			json.asJsonArray.forEach { jsonElement ->
				val obj = jsonElement.asJsonObject
				val name = obj.get("name").asString
				val parent = obj.get("parent")?.asString
				val pivot = obj.getAsJsonArray("pivot")?.map { it.asFloat } ?: listOf(0f, 0f, 0f)
				val rotation = obj.getAsJsonArray("rotation")?.map { it.asFloat } ?: listOf(0f, 0f, 0f)
				val cubes = obj.get("cubes")?.run { BrCube.parses(this) } ?: emptyList()

				// 解析骨骼级别定位器
				val locators = BrLocator.parses(obj)
				list.add(
					BrBone(
						name,
						parent,
						Vector3f(pivot[0], pivot[1], pivot[2]),
						Vector3f(rotation[0], rotation[1], rotation[2]),
						cubes,
						locators
					)
				)
			}
			return list
		}
	}
}
