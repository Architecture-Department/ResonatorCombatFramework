package architecture.resonator_combat_framework.module.entity_animation.engine

import com.google.gson.JsonObject
import org.joml.Vector3f

data class BrLocator(
	val name: String,
	val position: Vector3f = Vector3f()
) {

	companion object {
		@JvmStatic
		fun parses(obj: JsonObject?): MutableMap<String, BrLocator> {
			val locators = mutableMapOf<String, BrLocator>()
			obj?.getAsJsonObject("locators")?.let { locObj ->
				for ((locName, value) in locObj.entrySet()) {
					val position = value.asJsonArray?.map { it.asFloat } ?: listOf(0f, 0f, 0f)
					locators[locName] = BrLocator(locName, position.run { Vector3f(this[0], this[1], this[2]) })
				}
			}
			return locators
		}
	}
}