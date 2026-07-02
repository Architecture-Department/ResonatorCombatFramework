@file:Suppress("UNCHECKED_CAST")

package architecture.resonator_combat_framework.module.entity_animation.util

import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.cache.GeckoLibCache
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.loading.`object`.BakedAnimations

/**
 * GeckoLib 缓存工具类，根据客户端/服务端环境统一获取缓存数据。
 *
 * 客户端直接从 [GeckoLibCache] 获取，服务端从 [GeckoLibCacheServer] 获取。
 */
object GeckoLibUtil {
	/**
	 * 获取已烘焙的动画数据。
	 * @param isClient 是否为客户端环境
	 * @return 动画 ID 到 [BakedAnimations] 的映射
	 */
	@JvmStatic
	fun getAnimations(isClient: Boolean): Map<ResourceLocation, BakedAnimations> {
		return if (isClient) GeckoLibCache.getBakedAnimations() else GeckoLibCacheServer.ANIMATIONS
	}

	/**
	 * 获取已烘焙的模型数据。
	 * @param isClient 是否为客户端环境
	 * @return 模型 ID 到 [BakedGeoModel] 的映射
	 */
	@JvmStatic
	fun getModels(isClient: Boolean): Map<ResourceLocation, BakedGeoModel> {
		return if (isClient) GeckoLibCache.getBakedModels() else GeckoLibCacheServer.MODELS
	}
}
