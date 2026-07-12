package architecture.resonator_combat_framework.registry

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.animation.keyframe_animation.KeyframeAnimation
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
 * 关键帧动画注册表，从 `rcf/animations` 目录加载 JSON 动画数据。
 * 维护客户端和服务端两个独立实例，支持跨端查询和 NBT 缓存用于网络同步。
 * 解析 JSON 中的 "animations" 段，将每个动画条目注册为 [KeyframeAnimation]。
 *
 * @property isClient 是否为客户端实例
 */
class KeyframeAnimationRegistry(
	val isClient: Boolean
) : SimpleJsonResourceReloadListener(Gson(), "rcf/animations") {

	companion object {
		private val CLIENT = KeyframeAnimationRegistry(true)
		private val SERVER = KeyframeAnimationRegistry(false)

		/**
		 * 获取指定端的单例实例。
		 *
		 * @param isClient 是否为客户端
		 * @return 关键帧动画注册表实例
		 */
		@JvmStatic
		fun getInstance(isClient: Boolean): KeyframeAnimationRegistry {
			return if (isClient) CLIENT else SERVER
		}

		/**
		 * 跨端查找关键帧动画。
		 *
		 * @param animId 动画 ID
		 * @return 关键帧动画，不存在时返回 null
		 */
		@JvmStatic
		fun find(animId: ResourceLocation): KeyframeAnimation? {
			return CLIENT.get(animId) ?: SERVER.get(animId)
		}

		/**
		 * 获取所有端的合并动画数据。
		 *
		 * @return 所有关键帧动画的映射
		 */
		@JvmStatic
		fun findAll(): Map<ResourceLocation, KeyframeAnimation> = CLIENT.getAll() + SERVER.getAll()

		@JvmStatic
		fun get(isClient: Boolean, animId: ResourceLocation): KeyframeAnimation? {
			return if (isClient) find(animId) else SERVER.get(animId)
		}
	}

	private val bakingAnimations = mutableMapOf<ResourceLocation, KeyframeAnimation>()
	private val nbtCache = mutableMapOf<ResourceLocation, CompoundTag>()

	/**
	 * 获取指定 ID 的关键帧动画。
	 *
	 * @param animId 动画 ID
	 * @return 关键帧动画，不存在时返回 null
	 */
	fun get(animId: ResourceLocation): KeyframeAnimation? {
		return bakingAnimations[animId]
	}

	/**
	 * 获取所有关键帧动画。
	 *
	 * @return 所有关键帧动画的映射
	 */
	fun getAll(): Map<ResourceLocation, KeyframeAnimation> = bakingAnimations

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
	 * 应用解析后的动画数据。
	 *
	 * @param loaded 文件 ID 到 JSON 元素的映射
	 */
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
		AnimationDefRegistry.rebuild()
	}
}
