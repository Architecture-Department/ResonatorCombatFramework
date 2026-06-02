package architecture.resonator_combat_framework.module.entity_animation.engine

import com.google.gson.JsonElement

object BedrockModelParser {
	fun parse(json: JsonElement): List<BedrockModel> {
		val root = json.asJsonObject
		val result = mutableListOf<BedrockModel>()
		val geometryArray = root.getAsJsonArray("minecraft:geometry") ?: return result
		for (element in geometryArray) {
			val geo = element.asJsonObject
			val desc = geo.getAsJsonObject("description")
			val identifier = desc?.get("identifier")?.asString ?: "unknown"
			result.add(BedrockModel(identifier = identifier))
		}
		return result
	}
}
