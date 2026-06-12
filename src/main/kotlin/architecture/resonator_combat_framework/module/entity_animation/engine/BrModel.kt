package architecture.resonator_combat_framework.module.entity_animation.engine

import com.google.gson.JsonElement

data class BrModel(
	val identifier: String,
	val bones: List<BrBone> = emptyList(),
	val locators: Map<String, BrLocator> = emptyMap()
) {
	companion object {
		@JvmStatic
		fun parses(json: JsonElement): List<BrModel> {
			val root = json.asJsonObject
			val result = mutableListOf<BrModel>()
			val geometryArray = root.getAsJsonArray("minecraft:geometry") ?: return result
			for (element in geometryArray) {
				val obj = element.asJsonObject
				val desc = obj.getAsJsonObject("description")
				val identifier = desc?.get("identifier")?.asString ?: "unknown"
				val bones = obj.get("bones")?.run { BrBone.parses(this) } ?: emptyList()

				// 解析几何级别定位器
				val locators = BrLocator.parses(obj)

				// 解析骨骼级别的定位器
				for (bone in bones) {
					for ((name, loc) in bone.locators) {
						locators[name] = loc
					}
				}

				result.add(BrModel(identifier, bones, locators))
			}
			return result
		}
	}
}
