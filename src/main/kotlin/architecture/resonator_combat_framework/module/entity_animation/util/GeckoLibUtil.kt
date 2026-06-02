@file:Suppress("UNCHECKED_CAST")

package architecture.resonator_combat_framework.module.entity_animation.util

import architecture.resonator_combat_framework.module.entity_animation.GeckoLibCacheServer
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.cache.GeckoLibCache
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.loading.`object`.BakedAnimations

object GeckoLibUtil {
	@JvmStatic
	fun getAnimations(isClient: Boolean): Map<ResourceLocation, BakedAnimations> {
		return if (isClient) GeckoLibCache.getBakedAnimations() else GeckoLibCacheServer.ANIMATIONS
	}

	@JvmStatic
	fun getModels(isClient: Boolean): Map<ResourceLocation, BakedGeoModel> {
		return if (isClient) GeckoLibCache.getBakedModels() else GeckoLibCacheServer.MODELS
	}
}
