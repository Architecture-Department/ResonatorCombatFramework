package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.KeyframeAnimation
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
		fun find(animId: ResourceLocation): KeyframeAnimation? {
			return CLIENT.get(animId) ?: SERVER.get(animId)
		}

		@JvmStatic
		fun findAll(): Map<ResourceLocation, KeyframeAnimation> = CLIENT.getAll() + SERVER.getAll()
	}

	private val bakingAnimations = mutableMapOf<ResourceLocation, KeyframeAnimation>()
	private val nbtCache = mutableMapOf<ResourceLocation, CompoundTag>()

	fun get(animId: ResourceLocation): KeyframeAnimation? {
		return bakingAnimations[animId]
	}

	fun getAll(): Map<ResourceLocation, KeyframeAnimation> = bakingAnimations

	fun getNbtCache(): Map<ResourceLocation, CompoundTag> = nbtCache

	fun clearNbtCache() {
		nbtCache.clear()
	}

	override fun prepare(resourceManager: ResourceManager, profiler: ProfilerFiller): Map<ResourceLocation, JsonElement> {
		val map = super.prepare(resourceManager, profiler)
		for ((fileId, json) in map) {
			nbtCache[fileId] = JsonOps.COMPRESSED.convertTo(NbtOps.INSTANCE, json) as CompoundTag
		}
		return map
	}

	override fun apply(loaded: Map<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
		apply(loaded)
	}

	fun apply(loaded: Map<ResourceLocation, JsonElement>) {
		bakingAnimations.clear()
		for ((fileId, json) in loaded) {
			try {
				val parsed = KeyframeAnimation.parses(json.asJsonObject)
				parsed.forEach { (key, anim) ->
					if (!ResourceLocation.isValidPath(key)) {
						return@forEach
					}
					bakingAnimations[rlOf(
						fileId.namespace,
						fileId.path.substringBeforeLast("/") + "/" + key
					)] = anim
				}
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[ANIMATION] Failed to parse: {}", fileId, e)
			}
		}
		RcfUtil.LOGGER.info("[ANIMATION] Applied {} animations", bakingAnimations.size)

	}
}
