package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.engine.BrAnimation
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import com.google.gson.JsonParser
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller

/**
 * Bedrock 动画注册器。
 * 从 `assets/<namespace>/rcf/animations/` 加载动画 JSON 文件。
 * 每个 JSON 文件可包含多个动画（animations 对象下的所有条目均独立注册）。
 */
class BedrockAnimationRegistry(private val side: String = "?") :
	SimplePreparableReloadListener<Map<String, BrAnimation>>() {

	companion object {
		private val CLIENT = BedrockAnimationRegistry("CLIENT")
		private val SERVER = BedrockAnimationRegistry("SERVER")

		@JvmStatic
		fun getInstance(isClient: Boolean): BedrockAnimationRegistry {
			return if (isClient) CLIENT else SERVER
		}
	}

	private val animations = mutableMapOf<String, BrAnimation>()
	private val exprCache = mutableMapOf<String, MolangValue>()

	fun get(animId: String): BrAnimation? = animations[animId]

	fun getAllAnimIds(): Set<String> = animations.keys

	override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): Map<String, BrAnimation> {
		val result = mutableMapOf<String, BrAnimation>()
		var totalFiles = 0
		var totalAnims = 0
		for (entry in manager.listResources("rcf/animations") { it.path.endsWith(".json") }) {
			val before = result.size
			try {
				val json = JsonParser.parseReader(entry.value.openAsReader()).asJsonObject
				result.putAll(BrAnimation.parses(json, side, exprCache))
			} catch (e: Exception) {
				RcfConstants.LOGGER.error("[ANIMATION/{}] Failed to load: {} - {}", side, entry.key, e.message)
			}
			val loaded = result.size - before
			if (loaded > 0) {
				totalFiles += 1; totalAnims += loaded
				RcfConstants.LOGGER.info("[ANIMATION/{}] Loaded {} animations from {}", side, loaded, entry.key)
			}
		}
		RcfConstants.LOGGER.info("[ANIMATION/{}] Prepare complete: {} files, {} animations", side, totalFiles, totalAnims)
		return result
	}

	override fun apply(loaded: Map<String, BrAnimation>, manager: ResourceManager, profiler: ProfilerFiller) {
		animations.clear()
		animations.putAll(loaded)
		RcfConstants.LOGGER.info(
			"[ANIMATION/{}] Applied {} bedrock animations: {}",
			side,
			loaded.size,
			loaded.keys.sorted().joinToString(", ")
		)
	}
}

