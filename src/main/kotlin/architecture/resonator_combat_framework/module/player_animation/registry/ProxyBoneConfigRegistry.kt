package architecture.resonator_combat_framework.module.player_animation.registry

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.player_animation.flags.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.player_animation.flags.ProxyBoneFlags
import architecture.resonator_combat_framework.module.player_animation.flags.ProxyTimelineEntry
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller

class ProxyBoneConfigRegistry : SimplePreparableReloadListener<Map<String, ProxyBoneConfigData>>() {

	companion object {
		private val INSTANCE = ProxyBoneConfigRegistry()
		private val INSTANCE_SERVER = ProxyBoneConfigRegistry()

		@JvmStatic
		fun getInstance(isClient: Boolean): ProxyBoneConfigRegistry {
			return if (isClient) INSTANCE else INSTANCE_SERVER
		}
	}

	private val configs = mutableMapOf<String, ProxyBoneConfigData>()

	fun getConfig(animId: String): ProxyBoneConfigData {
		return configs[animId] ?: ProxyBoneConfigData.EMPTY
	}

	fun getAllAnimIds(): Set<String> = configs.keys

	override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): Map<String, ProxyBoneConfigData> {
		val result = mutableMapOf<String, ProxyBoneConfigData>()
		for (entry in manager.listResources("rcf/animdatas") { it.path.endsWith(".json") }) {
			val animId = entry.key.path.substringAfterLast("/").removeSuffix(".json")
			try {
				val json = JsonParser.parseReader(entry.value.openAsReader()).asJsonObject
				result[animId] = parseConfig(json)
			} catch (e: Exception) {
				RcfConstants.LOGGER.error("Failed to load bone config: {}", entry.key, e)
			}
		}
		return result
	}

	override fun apply(loaded: Map<String, ProxyBoneConfigData>, manager: ResourceManager, profiler: ProfilerFiller) {
		configs.clear()
		configs.putAll(loaded)
		RcfConstants.LOGGER.info("Loaded {} bone configs", loaded.size)
	}

	private fun parseConfig(json: JsonObject): ProxyBoneConfigData {
		val transitionTicks = json.get("transition")?.asInt ?: ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS
		val bones = parseBonesSection(json.get("bones"))
		val timelineJson = json.getAsJsonObject("timeline")
		val timeline = if (timelineJson != null) {
			val list = mutableListOf<ProxyTimelineEntry>()
			for ((timeRange, data) in timelineJson.entrySet()) {
				val parts = timeRange.split("-")
				val entry = data.asJsonObject
				list.add(
					ProxyTimelineEntry(
						from = parts.getOrNull(0)?.toFloatOrNull() ?: 0f,
						to = parts.getOrNull(1)?.toFloatOrNull() ?: 0f,
						bones = parseBonesSection(entry.get("bones"))
					)
				)
			}
			list
		} else emptyList()
		return ProxyBoneConfigData.create(bones, timeline, transitionTicks)
	}

	private fun parseBonesSection(section: JsonElement?): Map<String, ProxyBoneFlags> {
		if (section == null || !section.isJsonObject) return emptyMap()
		val obj = section.asJsonObject
		val result = mutableMapOf<String, ProxyBoneFlags>()
		for ((boneName, boneElement) in obj.entrySet()) {
			if (!boneElement.isJsonObject) continue
			val boneObj = boneElement.asJsonObject
			result[boneName] = ProxyBoneFlags(flattenBoneFlags(boneObj))
		}
		return result
	}

	private fun flattenBoneFlags(obj: JsonObject): Map<String, Boolean> {
		val flat = mutableMapOf<String, Boolean>()
		for ((key, value) in obj.entrySet()) {
			when {
				value.isJsonPrimitive -> flat[key] = value.asBoolean
				value.isJsonObject -> {
					for ((subKey, subValue) in value.asJsonObject.entrySet()) {
						if (subValue.isJsonPrimitive) flat["$key.$subKey"] = subValue.asBoolean
					}
				}
			}
		}
		return flat
	}
}
