package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.resonator_combat_framework.module.entity_animation.animation.data.ProxyBoneConfigData
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

class BedrockAnimationDataRegistry(
	val isClient: Boolean
) : SimpleJsonResourceReloadListener(Gson(), "rcf/animdatas") {

	companion object {
		private val CLIENT = BedrockAnimationDataRegistry(true)
		private val SERVER = BedrockAnimationDataRegistry(false)

		@JvmStatic
		fun getInstance(isClient: Boolean): BedrockAnimationDataRegistry {
			return if (isClient) CLIENT else SERVER
		}

		@JvmStatic
		fun get(animId: String): ProxyBoneConfigData? {
			return CLIENT.get(animId) ?: SERVER.get(animId)
		}

		@JvmStatic
		fun getAll(): Map<String, ProxyBoneConfigData> = CLIENT.getAll() + SERVER.getAll()
	}

	private val configs = mutableMapOf<String, ProxyBoneConfigData>()
	private val nbtCache = mutableMapOf<ResourceLocation, CompoundTag>()

	fun get(animId: String): ProxyBoneConfigData? {
		return configs[animId]
	}

	fun getAll(): Map<String, ProxyBoneConfigData> = configs

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
		configs.clear()
		for ((fileId, json) in loaded) {
			val animId = fileId.path
			try {
				configs[animId] = ProxyBoneConfigData.parse(json.asJsonObject, isClient)
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[CONFIG] Failed to parse: {}", fileId, e)
			}
		}
		RcfUtil.LOGGER.info("[CONFIG] Applied {} bone configs", configs.size)
	}
}
