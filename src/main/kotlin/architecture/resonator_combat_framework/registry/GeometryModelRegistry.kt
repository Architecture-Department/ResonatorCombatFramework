package architecture.resonator_combat_framework.registry

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.model.GeometryModel
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
 * 几何模型注册表，从 `rcf/models` 目录加载 JSON 几何模型数据。
 * 维护客户端和服务端两个独立实例，支持跨端查询和 NBT 缓存用于网络同步。
 * 支持通过完整 ID 或带 "geometry." 前缀的 ID 查找模型。
 *
 * @property isClient 是否为客户端实例
 */
class GeometryModelRegistry(
	val isClient: Boolean
) : SimpleJsonResourceReloadListener(Gson(), "rcf/models") {

	companion object {
		private val CLIENT = GeometryModelRegistry(true)
		private val SERVER = GeometryModelRegistry(false)

		/**
		 * 获取指定端的单例实例。
		 *
		 * @param isClient 是否为客户端
		 * @return 几何模型注册表实例
		 */
		@JvmStatic
		fun getInstance(isClient: Boolean): GeometryModelRegistry {
			return if (isClient) CLIENT else SERVER
		}

		/**
		 * 跨端查找几何模型。
		 *
		 * @param identifier 模型标识符
		 * @return 几何数据，不存在时返回 null
		 */
		@JvmStatic
		fun find(identifier: ResourceLocation): GeometryModel? {
			return CLIENT.get(identifier) ?: SERVER.get(identifier)
		}

		/**
		 * 获取所有端的合并模型数据。
		 *
		 * @return 所有几何数据的映射
		 */
		@JvmStatic
		fun findAll(): Map<ResourceLocation, GeometryModel> = CLIENT.getAll() + SERVER.getAll()

		@JvmStatic
		fun get(isClient: Boolean, id: ResourceLocation): GeometryModel? {
			return if (isClient) find(id) else SERVER.get(id)
		}
	}

	private val models = mutableMapOf<ResourceLocation, GeometryModel>()
	private val nbtCache = mutableMapOf<ResourceLocation, CompoundTag>()

	/**
	 * 获取指定标识符的几何模型。
	 * 优先精确匹配，失败时尝试添加 "geometry." 前缀查找。
	 *
	 * @param identifier 模型标识符
	 * @return 几何数据，不存在时返回 null
	 */
	fun get(identifier: ResourceLocation): GeometryModel? {
		val original = identifier.path
		return models[identifier] ?: models[rlOf(
			identifier.namespace,
			original.replaceAfterLast("/", "geometry." + original.substringAfterLast("/"))
		)]
	}

	/**
	 * 获取所有几何模型。
	 *
	 * @return 所有几何数据的映射
	 */
	fun getAll(): Map<ResourceLocation, GeometryModel> = models

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
	 * 应用解析后的模型数据。
	 *
	 * @param loaded 文件 ID 到 JSON 元素的映射
	 */
	fun apply(loaded: Map<ResourceLocation, JsonElement>) {
		models.clear()
		for ((fileId, json) in loaded) {
			try {
				GeometryModel.parses(json).forEach { model ->
					if (!ResourceLocation.isValidPath(model.identifier)) {
						return@forEach
					}
					models[rlOf(fileId.namespace, fileId.path.substringBeforeLast("/") + "/" + model.identifier)] = model
				}
			} catch (e: Exception) {
				RcfUtil.LOGGER.error("[MODEL] Failed to parse: {}", json, e)
			}
		}
		RcfUtil.LOGGER.info("[MODEL] Applied {} models", models.size)
	}
}
