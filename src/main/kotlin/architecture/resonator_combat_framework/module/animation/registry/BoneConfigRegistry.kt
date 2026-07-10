package architecture.resonator_combat_framework.module.animation.registry

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.module.animation.data.BoneConfig
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

/**
 * 骨骼配置注册表，从 `rcf/animdatas` 目录加载 JSON 骨骼配置。
 * 维护客户端和服务端两个独立实例，支持跨端查询和 NBT 缓存用于网络同步。
 *
 * @property isClient 是否为客户端实例
 */
class BoneConfigRegistry(
	val isClient: Boolean
) : SimpleJsonResourceReloadListener(Gson(), "rcf/animdatas") {

	companion object {
		private val CLIENT = BoneConfigRegistry(true)
		private val SERVER = BoneConfigRegistry(false)

		/**
		 * 获取指定端的单例实例。
		 *
		 * @param isClient 是否为客户端
		 * @return 骨骼配置注册表实例
		 */
		@JvmStatic
		fun getInstance(isClient: Boolean): BoneConfigRegistry {
			return if (isClient) CLIENT else SERVER
		}

		/**
		 * 跨端查找骨骼配置。
		 *
		 * @param animId 动画 ID
		 * @return 骨骼配置，不存在时返回 null
		 */
		@JvmStatic
		fun find(animId: ResourceLocation): BoneConfig? {
			return CLIENT.get(animId) ?: SERVER.get(animId)
		}

		/**
		 * 获取所有端的合并配置。
		 *
		 * @return 所有骨骼配置的映射
		 */
		@JvmStatic
		fun findAll(): Map<ResourceLocation, BoneConfig> = CLIENT.getAll() + SERVER.getAll()

		@JvmStatic
		fun get(isClient: Boolean, animId: ResourceLocation): BoneConfig? {
			return if (isClient) find(animId) else SERVER.get(animId)
		}
	}

	private val configs = mutableMapOf<ResourceLocation, BoneConfig>()
	private val nbtCache = mutableMapOf<ResourceLocation, CompoundTag>()

	/**
	 * 获取指定动画的骨骼配置。
	 *
	 * @param animId 动画 ID
	 * @return 骨骼配置，不存在时返回 null
	 */
	fun get(animId: ResourceLocation): BoneConfig? {
		return configs[animId]
	}

	/**
	 * 获取所有骨骼配置。
	 *
	 * @return 所有骨骼配置的映射
	 */
	fun getAll(): Map<ResourceLocation, BoneConfig> = configs

	/**
	 * 获取 NBT 缓存（用于网络同步）。
	 *
	 * @return NBT 缓存映射
	 */
	fun getNbtCache(): Map<ResourceLocation, CompoundTag> = nbtCache

	/** 清空 NBT 缓存 */
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

	/**
	 * 应用解析后的配置数据。
	 *
	 * @param loaded 文件 ID 到 JSON 元素的映射
	 */
	fun apply(loaded: Map<ResourceLocation, JsonElement>) {
		configs.clear()
		for ((fileId, json) in loaded) {
			try {
				val parse = BoneConfig.parse(json.asJsonObject)
				configs[rlOf(fileId.namespace, fileId.path)] = parse
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[CONFIG] Failed to parse: {}", fileId, e)
			}
		}
		RcfUtil.LOGGER.info("[CONFIG] Applied {} bone configs", configs.size)
	}
}
