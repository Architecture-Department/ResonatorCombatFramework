package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.engine.BrModel
import com.google.gson.JsonParser
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller

/**
 * Bedrock 模型注册器。
 * 从 `assets/<namespace>/rcf/models/` 加载模型 JSON 文件。
 * 每个 JSON 文件可包含多个模型（minecraft:geometry[] 下的所有条目），
 * 使用模型自身的 identifier（如 "geometry.default"）作为注册键。
 */
class BedrockModelRegistry(private val side: String = "?") :
	SimplePreparableReloadListener<Map<String, BrModel>>() {

	companion object {
		private val CLIENT = BedrockModelRegistry("CLIENT")
		private val SERVER = BedrockModelRegistry("SERVER")

		@JvmStatic
		fun getInstance(isClient: Boolean): BedrockModelRegistry {
			return if (isClient) CLIENT else SERVER
		}
	}

	private val models = mutableMapOf<String, BrModel>()

	/** 按模型 identifier（如 "geometry.default"）获取模型 */
	fun get(identifier: String): BrModel? = models[identifier]

	fun getAllModelIds(): Set<String> = models.keys

	fun getAllModels(): Map<String, BrModel> = models.toMap()

	override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): Map<String, BrModel> {
		val result = mutableMapOf<String, BrModel>()
		for (entry in manager.listResources("rcf/models") { it.path.endsWith(".json") }) {
			try {
				val json = JsonParser.parseReader(entry.value.openAsReader())
				val parsedModels = BrModel.parse(json)
				for (model in parsedModels) {
					result[model.identifier] = model
				}
			} catch (e: Exception) {
				RcfConstants.LOGGER.error("[MODEL/{}] Failed to load: {} - {}", side, entry.key, e.message)
			}
		}
		return result
	}

	override fun apply(loaded: Map<String, BrModel>, manager: ResourceManager, profiler: ProfilerFiller) {
		models.clear()
		models.putAll(loaded)
		RcfConstants.LOGGER.info(
			"[MODEL/{}] Applied {} bedrock models: {}",
			side,
			loaded.size,
			loaded.keys.sorted().joinToString(", ")
		)
	}
}

