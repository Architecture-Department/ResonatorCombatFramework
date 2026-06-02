package architecture.resonator_combat_framework.module.entity_animation


import architecture.resonator_combat_framework.mixin.gecko_lib.GeckoLibCacheAccessor
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.loading.`object`.BakedAnimations
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Consumer
import java.util.function.Function

// 服务端 GeckoLib 缓存。存储服务端的模型(BakedGeoModel)和动画(BakedAnimations)数据，通过 Mixin 访问 GeckoLib 的加载逻辑
object GeckoLibCacheServer {
	@JvmField
	val ANIMATIONS = Object2ObjectOpenHashMap<ResourceLocation, BakedAnimations>()

	@JvmField
	val MODELS = Object2ObjectOpenHashMap<ResourceLocation, BakedGeoModel>()

	@JvmStatic
	fun reload(
		stage: PreparableReloadListener.PreparationBarrier,
		resourceManager: ResourceManager,
		preparationsProfiler: ProfilerFiller?,
		reloadProfiler: ProfilerFiller?,
		backgroundExecutor: Executor?,
		gameExecutor: Executor?
	): CompletableFuture<Void?> {
		val animations = Object2ObjectOpenHashMap<ResourceLocation, BakedAnimations>()
		val models = Object2ObjectOpenHashMap<ResourceLocation, BakedGeoModel>()

		return CompletableFuture.allOf(
			GeckoLibCacheAccessor.callLoadAnimations(
				backgroundExecutor,
				resourceManager
			) { key, value -> animations[key] = value },
			GeckoLibCacheAccessor.callLoadModels(
				backgroundExecutor,
				resourceManager
			) { key, value -> models[key] = value }
		)
			.thenCompose(Function { backgroundResult -> stage.wait(backgroundResult) })
			.thenAcceptAsync(Consumer { empty ->
				ANIMATIONS.clear()
				ANIMATIONS.putAll(animations)
				MODELS.clear()
				MODELS.putAll(models)
			}, gameExecutor)
	}
}
