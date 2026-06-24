package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.resonator_combat_framework.module.entity_animation.animation.BakingBrAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue
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
	private val exprCache = mutableMapOf<String, MolangValue>()
	private val staticAnimation = mutableMapOf<String, StaticAnimation>()

	fun getBakingAnimation(animId: String): BakingBrAnimation? = bakingAnimations[animId]
	fun getStaticAnimation(animId: String): StaticAnimation? = staticAnimation[animId]
	fun getAllAnimIds(): Set<String> = bakingAnimations.keys
	fun getAllAnim(): Map<String, BakingBrAnimation> = bakingAnimations
	fun getAllStaticAnim(): Map<String, StaticAnimation> = staticAnimation

	override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): Map<String, BakingBrAnimation> {
		val result = mutableMapOf<String, BakingBrAnimation>()
		var totalFiles = 0
		var totalAnims = 0
		for (entry in manager.listResources("rcf/animations") { it.path.endsWith(".json") }) {
			val before = result.size
			try {
				val json = JsonParser.parseReader(entry.value.openAsReader()).asJsonObject
				result.putAll(BakingBrAnimation.parses(json, side, exprCache))
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
		staticAnimation.clear()
		for (animId in bakingAnimations.keys) {
			staticAnimation[animId] = StaticAnimation(RcfUtil.modRl(animId), animId)
		}
		RcfUtil.LOGGER.info(
			"[ANIMATION/{}] Applied {} bedrock animations: {}",
			side,
			loaded.size,
			loaded.keys.sorted().joinToString(", ")
		)
	}
}

