package architecture.resonator_combat_framework.module.player_animation.config

import architecture.resonator_combat_framework.core.Rcf
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.github.tt432.eyelib.client.loader.BrResourcesLoader
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

class ProxyBoneConfigLoader : BrResourcesLoader("animdata", "json") {
	companion object {
		private val INSTANCE = ProxyBoneConfigLoader()

		private val INSTANCE_SERVER = ProxyBoneConfigLoader()

		@JvmStatic
		fun getInstance(isClient: Boolean): ProxyBoneConfigLoader {
			return if (isClient) INSTANCE else INSTANCE_SERVER
		}
	}

	private val configs = mutableMapOf<String, ProxyBoneConfigData>()

	fun getConfig(animId: String): ProxyBoneConfigData {
		return configs[animId] ?: ProxyBoneConfigData.EMPTY
	}

	override fun apply(
		loaded: MutableMap<ResourceLocation, JsonElement>,
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	) {
		val map = mutableMapOf<String, ProxyBoneConfigData>()
		loaded.forEach { (rl, json) ->
			try {
				map[rl.path.substringAfterLast("/")] = parse(json.asJsonObject)
			} catch (e: Exception) {
				Rcf.LOGGER.error("can't load bone config {}", rl, e)
			}
		}
		configs.putAll(map)
	}

	private fun parse(json: JsonObject): ProxyBoneConfigData {
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
		return ProxyBoneConfigData(bones, timeline, transitionTicks)
	}

	private fun parseBonesSection(section: JsonElement?): Map<String, ProxyBoneFlags> {
		if (section == null || !section.isJsonObject) return emptyMap()
		val obj = section.asJsonObject
		val result = mutableMapOf<String, ProxyBoneFlags>()
		for ((boneName, flagsElement) in obj.entrySet()) {
			if (!flagsElement.isJsonObject) continue
			val flagsObj = flagsElement.asJsonObject
			val flags = mutableMapOf<String, Boolean>()
			for ((key, value) in flagsObj.entrySet()) {
				flags[key] = value.asBoolean
			}
			result[boneName] = ProxyBoneFlags(flags)
		}
		return result
	}
}