package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.BakingBrAnimation
import architecture.resonator_combat_framework.util.RcfUtil
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller

class BedrockAnimationRegistry(
	val isClient: Boolean
) : SimpleJsonResourceReloadListener(Gson(), "rcf/animations") {

	companion object {
		private val CLIENT = BedrockAnimationRegistry(true)
		private val SERVER = BedrockAnimationRegistry(false)

		@JvmStatic
		fun getInstance(isClient: Boolean): BedrockAnimationRegistry {
			return if (isClient) CLIENT else SERVER
		}

		@JvmStatic
		fun find(animId: String): BakingBrAnimation? {
			return CLIENT.get(animId) ?: SERVER.get(animId)
		}

		@JvmStatic
		fun findAll(): Map<String, BakingBrAnimation> = CLIENT.getAll() + SERVER.getAll()
	}

	private val bakingAnimations = mutableMapOf<String, BakingBrAnimation>()
	private val nbtCache = mutableMapOf<ResourceLocation, CompoundTag>()

	fun get(animId: String): BakingBrAnimation? {
		return bakingAnimations[animId]
	}

	fun getAll(): Map<String, BakingBrAnimation> = bakingAnimations

	fun getNbtCache(): Map<ResourceLocation, CompoundTag> = nbtCache

	fun clearNbtCache() {
		nbtCache.clear()
	}

	override fun prepare(resourceManager: ResourceManager, profiler: ProfilerFiller): Map<ResourceLocation, JsonElement> {
		val map = super.prepare(resourceManager, profiler)
		for ((fileId, json) in map) {
			nbtCache[fileId] = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, json) as CompoundTag
		}
		return map
	}

	override fun apply(loaded: Map<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
		bakingAnimations.clear()
		for ((fileId, json) in loaded) {
			try {
				bakingAnimations.putAll(BakingBrAnimation.parses(json.asJsonObject, if (isClient) "CLIENT" else "SERVER"))
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[ANIMATION] Failed to parse: {}", fileId, e)
			}
		}
		RcfUtil.LOGGER.info("[ANIMATION] Applied {} animations", bakingAnimations.size)
	}
}