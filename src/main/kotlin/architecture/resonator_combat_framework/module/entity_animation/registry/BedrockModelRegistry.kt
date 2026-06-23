package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.resonator_combat_framework.module.entity_animation.animation.model.BakingBrModel
import architecture.resonator_combat_framework.util.RcfUtil
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
class BedrockModelRegistry(
	val isClient: Boolean,
	private val side: String = if (isClient) "CLIENT" else "SERVER"
) :
	SimplePreparableReloadListener<Map<String, BakingBrModel>>() {

	companion object {
		private val CLIENT = BedrockModelRegistry(true)
		private val SERVER = BedrockModelRegistry(false)

		@JvmStatic
		fun getInstance(isClient: Boolean): BedrockModelRegistry {
			return if (isClient) CLIENT else SERVER
		}
	}

	private val models = mutableMapOf<String, BakingBrModel>()

	/** 按模型 identifier（如 "geometry.default"）获取模型 */
	fun get(identifier: String): BakingBrModel? = models[identifier] ?: models["geometry.${identifier}"]

	fun getAllModelIds(): Set<String> = models.keys

	fun getAllModels(): Map<String, BakingBrModel> = models.toMap()

	override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): Map<String, BakingBrModel> {
		val result = mutableMapOf<String, BakingBrModel>()
		for (entry in manager.listResources("rcf/models") { it.path.endsWith(".json") }) {
			try {
				val json = JsonParser.parseReader(entry.value.openAsReader())
				val parsedModels = BakingBrModel.parses(json)
				for (model in parsedModels) {
					result[model.identifier] = model
				}
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[MODEL/{}] Failed to load: {}", side, entry.key, e)
			}
		}
		return result
	}

	override fun apply(loaded: Map<String, BakingBrModel>, manager: ResourceManager, profiler: ProfilerFiller) {
		models.clear()
		models.putAll(loaded)
		RcfUtil.LOGGER.info(
			"[MODEL/{}] Applied {} bedrock models: {}",
			side,
			loaded.size,
			loaded.keys.sorted().joinToString(", ")
		)
	}
}

