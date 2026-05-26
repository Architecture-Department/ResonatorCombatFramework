package architecture.resonator_combat_framework.module.player_animation.config

import architecture.resonator_combat_framework.core.RcfConstants
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
				map[rl.path.substringAfterLast("/").removeSuffix(".json")] = parse(json.asJsonObject)
			} catch (e: Exception) {
				RcfConstants.LOGGER.error("can't load bone config {}", rl, e)
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

	/** 解析 bones 块：嵌套 JSON → 扁平 dot-notation Map */
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

	/** 将嵌套 JSON 对象打平为 dot-notation Map */
	private fun flattenBoneFlags(obj: JsonObject): Map<String, Boolean> {
		val flat = mutableMapOf<String, Boolean>()
		for ((key, value) in obj.entrySet()) {
			when {
				value.isJsonPrimitive -> {
					flat[key] = value.asBoolean
				}

				value.isJsonObject -> {
					val sub = value.asJsonObject
					for ((subKey, subValue) in sub.entrySet()) {
						if (subValue.isJsonPrimitive) {
							flat["$key.$subKey"] = subValue.asBoolean
						}
					}
				}
			}
		}
		return flat
	}
}
