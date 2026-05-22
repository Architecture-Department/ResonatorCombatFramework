package architecture.resonator_combat_framework.module.player_animation.config

import architecture.resonator_combat_framework.core.Rcf
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.github.tt432.eyelib.client.loader.BrResourcesLoader
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller

object RcfBoneConfigLoader : BrResourcesLoader("animdata", "json") {

	@Volatile
	private var configs: Map<String, RcfBoneConfig> = emptyMap()

	fun getConfig(animId: String): RcfBoneConfig {
		return configs[animId] ?: RcfBoneConfig.EMPTY
	}

	// 服务器端加载也调用
	fun loadConfigs(loaded: Map<ResourceLocation, JsonElement>) {
		val map = mutableMapOf<String, RcfBoneConfig>()
		loaded.forEach { (rl, json) ->
			try {
				map[rl.path] = parse(json.asJsonObject)
			} catch (e: Exception) {
				Rcf.LOGGER.error("can't load bone config {}", rl, e)
			}
		}
		configs = map
	}

	override fun apply(
		loaded: MutableMap<ResourceLocation, JsonElement>,
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	) {
		loadConfigs(loaded)
	}

	private fun parse(json: JsonObject): RcfBoneConfig {
		val bones = parseBonesSection(json.get("bones"))
		val timelineJson = json.getAsJsonObject("timeline") ?: return RcfBoneConfig(bones, emptyList())
		val timeline = mutableListOf<RcfTimelineEntry>()
		for ((timeRange, data) in timelineJson.entrySet()) {
			val parts = timeRange.split("-")
			val entry = data.asJsonObject
			timeline.add(
				RcfTimelineEntry(
					from = parts.getOrNull(0)?.toFloatOrNull() ?: 0f,
					to = parts.getOrNull(1)?.toFloatOrNull() ?: 0f,
					bones = parseBonesSection(entry.get("bones"))
				)
			)
		}
		return RcfBoneConfig(bones, timeline)
	}

	private fun parseBonesSection(section: JsonElement?): Map<String, RcfBoneFlags> {
		if (section == null || !section.isJsonObject) return emptyMap()
		val obj = section.asJsonObject
		val result = mutableMapOf<String, RcfBoneFlags>()
		for ((boneName, flagsElement) in obj.entrySet()) {
			if (!flagsElement.isJsonObject) continue
			val flagsObj = flagsElement.asJsonObject
			val flags = mutableMapOf<String, Boolean>()
			for ((key, value) in flagsObj.entrySet()) {
				flags[key] = value.asBoolean
			}
			result[boneName] = RcfBoneFlags(flags)
		}
		return result
	}
}

object ServerBoneConfigLoader : SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>>() {
	private val gson = GsonBuilder().setLenient().create()

	override fun prepare(resourceManager: ResourceManager, profiler: ProfilerFiller): Map<ResourceLocation, JsonElement> {
		val map = mutableMapOf<ResourceLocation, JsonElement>()
		val converter = FileToIdConverter("eyelib/animdata", ".json")
		for ((rl, resource) in converter.listMatchingResources(resourceManager)) {
			val id = converter.fileToId(rl)
			try {
				resource.openAsReader().use { reader ->
					val element = gson.fromJson(reader, JsonElement::class.java)
					map[id] = element
				}
			} catch (e: Exception) {
				Rcf.LOGGER.error("can't load server bone config {}", id, e)
			}
		}
		return map
	}

	override fun apply(
		loaded: Map<ResourceLocation, JsonElement>,
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	) {
		RcfBoneConfigLoader.loadConfigs(loaded)
	}
}