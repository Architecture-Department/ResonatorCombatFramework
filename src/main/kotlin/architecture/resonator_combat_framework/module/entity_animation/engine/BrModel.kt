package architecture.resonator_combat_framework.module.entity_animation.engine

import com.google.gson.JsonElement

data class BrModel(
	val identifier: String,
	val bones: List<BrBone> = emptyList()
) {
	companion object {
		@JvmStatic
		fun parse(json: JsonElement): List<BrModel> {
			val root = json.asJsonObject
			val result = mutableListOf<BrModel>()
			val geometryArray = root.getAsJsonArray("minecraft:geometry") ?: return result
			for (element in geometryArray) {
				val geo = element.asJsonObject
				val desc = geo.getAsJsonObject("description")
				val identifier = desc?.get("identifier")?.asString ?: "unknown"
				val bones = geo.get("bones")?.run { BrBone.parses(this) } ?: emptyList()
				result.add(BrModel(identifier, bones))
			}
			return result
		}
	}
}