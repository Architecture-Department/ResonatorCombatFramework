package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.resonator_combat_framework.module.entity_animation.animation.data.ProxyBoneConfigData
import architecture.resonator_combat_framework.util.RcfUtil
import com.google.gson.JsonParser
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller

class ProxyBoneConfigDataRegistry(
	val isClient: Boolean,
	private val side: String = if (isClient) "CLIENT" else "SERVER"
) :
	SimplePreparableReloadListener<Map<String, ProxyBoneConfigData>>() {

	companion object {
		private val CLIENT = ProxyBoneConfigDataRegistry(true)
		private val SERVER = ProxyBoneConfigDataRegistry(false)

		@JvmStatic
		fun getInstance(isClient: Boolean): ProxyBoneConfigDataRegistry {
			return if (isClient) CLIENT else SERVER
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
				result[animId] = ProxyBoneConfigData.parse(json, isClient)
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[CONFIG/{}] Failed to load: {}", side, entry.key, e)
			}
		}
		return result
	}

	override fun apply(loaded: Map<String, ProxyBoneConfigData>, manager: ResourceManager, profiler: ProfilerFiller) {
		configs.clear()
		configs.putAll(loaded)
		RcfUtil.LOGGER.info(
			"[CONFIG/{}] Applied {} bone configs: {}",
			side,
			loaded.size,
			loaded.keys.sorted().joinToString(", ")
		)
	}
}
