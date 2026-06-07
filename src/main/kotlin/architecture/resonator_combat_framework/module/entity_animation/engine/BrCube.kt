package architecture.resonator_combat_framework.module.entity_animation.engine

import com.google.gson.JsonElement
import org.joml.Vector3f

data class BrCube(
	val inflate: Float = 0f,
	val origin: Vector3f = Vector3f(),
	val size: Vector3f = Vector3f(),
	val rotation: Vector3f = Vector3f()
) {
	companion object {
		@JvmStatic
		fun parses(json: JsonElement): List<BrCube> {
			val list = mutableListOf<BrCube>()
			json.asJsonArray.forEach {
				val obj = it.asJsonObject
				val origin = obj.getAsJsonArray("origin")?.map { it1 -> it1.asFloat } ?: listOf(0f, 0f, 0f)
				val size = obj.getAsJsonArray("size")?.map { it1 -> it1.asFloat } ?: listOf(0f, 0f, 0f)
				val inflate = obj.get("inflate")?.asFloat ?: 0f
				val rotation = obj.getAsJsonArray("size")?.map { it1 -> it1.asFloat } ?: listOf(0f, 0f, 0f)
				list.add(
					BrCube(
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