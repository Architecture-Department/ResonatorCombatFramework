package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.goldenboughs_lib.util.LibUtil
import architecture.resonator_combat_framework.module.entity_animation.animation.model.GeometryData
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

class GeometryModelRegistry(
	val isClient: Boolean
) : SimpleJsonResourceReloadListener(Gson(), "rcf/models") {

	companion object {
		private val CLIENT = GeometryModelRegistry(true)
		private val SERVER = GeometryModelRegistry(false)

		@JvmStatic
		fun getInstance(isClient: Boolean): GeometryModelRegistry {
			return if (isClient) CLIENT else SERVER
		}

		@JvmStatic
		fun find(identifier: ResourceLocation): GeometryData? {
			return CLIENT.get(identifier) ?: SERVER.get(identifier)
		}

		@JvmStatic
		fun findAll(): Map<ResourceLocation, GeometryData> = CLIENT.getAll() + SERVER.getAll()
	}

	private val models = mutableMapOf<ResourceLocation, GeometryData>()
	private val nbtCache = mutableMapOf<ResourceLocation, CompoundTag>()

	fun get(identifier: ResourceLocation): GeometryData? {
		return models[identifier] ?: models[LibUtil.rlOf(identifier.namespace, "geometry.${identifier.path}")]
	}

	fun getAll(): Map<ResourceLocation, GeometryData> = models

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
		models.clear()
		for ((fileId, json) in loaded) {
			try {
				GeometryData.parses(json).forEach { model ->
					if (!ResourceLocation.isValidPath(model.identifier)) {
						return@forEach
					}
					models[LibUtil.rlOf(fileId.namespace, fileId.path.substringBeforeLast("/") + "/" + model.identifier)] = model
				}
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[MODEL] Failed to parse: {}", json, e)
			}
		}
		RcfUtil.LOGGER.info("[MODEL] Applied {} models", models.size)
	}
}
