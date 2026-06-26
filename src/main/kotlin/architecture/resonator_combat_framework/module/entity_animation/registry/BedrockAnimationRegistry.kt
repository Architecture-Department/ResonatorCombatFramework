package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.resonator_combat_framework.event.AnimationRegistry
import architecture.resonator_combat_framework.init.RcfRegistries
import architecture.resonator_combat_framework.init.RcfStaticAnimations
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.BakingBrAnimation
import architecture.resonator_combat_framework.util.RcfUtil
import com.google.gson.JsonParser
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller

/**
 * Bedrock 动画注册器。
 * 从 `assets/<namespace>/rcf/animations/` 加载动画 JSON 文件。
 * 每个 JSON 文件可包含多个动画（animations 对象下的所有条目均独立注册）。
 */
class BedrockAnimationRegistry(
	val isClient: Boolean,
	private val side: String = if (isClient) "CLIENT" else "SERVER"
) : SimplePreparableReloadListener<Map<String, BakingBrAnimation>>() {

	companion object {
		private val CLIENT = BedrockAnimationRegistry(true)
		private val SERVER = BedrockAnimationRegistry(false)

		@JvmStatic
		fun getInstance(isClient: Boolean): BedrockAnimationRegistry {
			return if (isClient) CLIENT else SERVER
		}
	}

	private val bakingAnimations = mutableMapOf<String, BakingBrAnimation>()

	fun getBakingAnimation(animId: String): BakingBrAnimation? = bakingAnimations[animId]

	fun getAllAnimIds(): Set<String> = bakingAnimations.keys

	fun getAllAnim(): Map<String, BakingBrAnimation> = bakingAnimations

	override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): Map<String, BakingBrAnimation> {
		val result = mutableMapOf<String, BakingBrAnimation>()
		var totalFiles = 0
		var totalAnims = 0
		for (entry in manager.listResources("rcf/animations") { it.path.endsWith(".json") }) {
			val before = result.size
			try {
				val json = JsonParser.parseReader(entry.value.openAsReader()).asJsonObject
				result.putAll(BakingBrAnimation.parses(json, side))
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[ANIMATION/{}] Failed to load: {}", side, entry.key, e)
			}
			val loaded = result.size - before
			if (loaded > 0) {
				totalFiles += 1; totalAnims += loaded
				RcfUtil.LOGGER.info("[ANIMATION/{}] Loaded {} animations from {}", side, loaded, entry.key)
			}
		}
		RcfUtil.LOGGER.info("[ANIMATION/{}] Prepare complete: {} files, {} animations", side, totalFiles, totalAnims)
		return result
	}

	override fun apply(loaded: Map<String, BakingBrAnimation>, manager: ResourceManager, profiler: ProfilerFiller) {
		bakingAnimations.clear()
		bakingAnimations.putAll(loaded)

		RcfUtil.LOGGER.info(
			"[ANIMATION/{}] Applied {} bedrock animations: {}",
			side,
			loaded.size,
			loaded.keys.sorted().joinToString(", ")
		)

		RcfRegistries.getStaticAnimations(isClient).clear()
		getInstance(isClient).getAllAnim().forEach { (id, animation) ->
			RcfStaticAnimations.register(id, { _ -> StaticAnimation(id) }, isClient)
		}
		(if (isClient) AnimationRegistry.CLIENTS else AnimationRegistry.SERVERS).forEach { it() }
		RcfRegistries.getStaticAnimations(isClient).forEach { (_, animation) ->
			animation.init(isClient)
		}
	}
}

