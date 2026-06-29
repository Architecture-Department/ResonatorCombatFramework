package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.resonator_combat_framework.module.entity_animation.animation.model.BakingBrModel
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

class BedrockModelRegistry(
	val isClient: Boolean
) : SimpleJsonResourceReloadListener(Gson(), "rcf/models") {

	companion object {
		private val CLIENT = BedrockModelRegistry(true)
		private val SERVER = BedrockModelRegistry(false)

		@JvmStatic
		fun getInstance(isClient: Boolean): BedrockModelRegistry {
			return if (isClient) CLIENT else SERVER
		}

		@JvmStatic
		fun find(identifier: String): BakingBrModel? {
			return CLIENT.get(identifier) ?: SERVER.get(identifier)
		}

		@JvmStatic
		fun findAll(): Map<String, BakingBrModel> = CLIENT.getAll() + SERVER.getAll()
	}

	private val models = mutableMapOf<String, BakingBrModel>()
	private val nbtCache = mutableMapOf<ResourceLocation, CompoundTag>()

	fun get(identifier: String): BakingBrModel? {
		return models[identifier] ?: models["geometry.${identifier}"]
	}

	fun getAll(): Map<String, BakingBrModel> = models

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
		models.clear()
		for ((_, json) in loaded) {
			try {
				for (model in BakingBrModel.parses(json)) {
					models[model.identifier] = model
				}
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[MODEL] Failed to parse: {}", json, e)
			}
		}
		RcfUtil.LOGGER.info("[MODEL] Applied {} models", models.size)
	}
}
