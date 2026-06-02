package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.engine.BedrockModel
import architecture.resonator_combat_framework.module.entity_animation.engine.BedrockModelParser
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
class BedrockModelRegistry : SimplePreparableReloadListener<Map<String, BedrockModel>>() {

	companion object {
		private val CLIENT = BedrockModelRegistry()
		private val SERVER = BedrockModelRegistry()

		@JvmStatic
		fun getInstance(isClient: Boolean): BedrockModelRegistry {
			return if (isClient) CLIENT else SERVER
		}
	}

	private val models = mutableMapOf<String, BedrockModel>()

	/** 按模型 identifier（如 "geometry.default"）获取模型 */
	fun get(identifier: String): BedrockModel? = models[identifier]

	fun getAllModelIds(): Set<String> = models.keys

	fun getAllModels(): Map<String, BedrockModel> = models.toMap()

	override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): Map<String, BedrockModel> {
		val result = mutableMapOf<String, BedrockModel>()
		for (entry in manager.listResources("rcf/models") { it.path.endsWith(".json") }) {
			try {
				val json = JsonParser.parseReader(entry.value.openAsReader())
				val parsedModels = BedrockModelParser.parse(json)
				for (model in parsedModels) {
					result[model.identifier] = model
				}
			} catch (e: Exception) {
				RcfConstants.LOGGER.error("Failed to load model: {}", entry.key, e)
			}
		}
		return result
	}

	override fun apply(loaded: Map<String, BedrockModel>, manager: ResourceManager, profiler: ProfilerFiller) {
		models.clear()
		models.putAll(loaded)
		RcfConstants.LOGGER.info("Loaded {} bedrock models", loaded.size)
	}
}

