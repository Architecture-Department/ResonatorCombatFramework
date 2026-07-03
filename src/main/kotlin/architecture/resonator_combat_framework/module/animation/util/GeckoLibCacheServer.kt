package architecture.resonator_combat_framework.module.animation.util

import architecture.resonator_combat_framework.mixin.gecko_lib.GeckoLibCacheAccessor
import architecture.resonator_combat_framework.module.animation.util.GeckoLibCacheServer.ANIMATIONS
import architecture.resonator_combat_framework.module.animation.util.GeckoLibCacheServer.MODELS
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

/**
 * 服务端 GeckoLib 缓存，存储服务端侧的模型（[BakedGeoModel]）和动画（[BakedAnimations]）数据。
 *
 * 由于 GeckoLib 原版缓存仅在客户端可用，此对象通过 Mixin 访问 GeckoLib 的加载逻辑，
 * 在服务端数据包重载时同步加载并缓存模型与动画数据，供服务端动画系统的逻辑运算使用。
 */
object GeckoLibCacheServer {

	/** 缓存的动画数据映射表 */
	@JvmField
	val ANIMATIONS = Object2ObjectOpenHashMap<ResourceLocation, BakedAnimations>()

	/** 缓存的模型数据映射表 */
	@JvmField
	val MODELS = Object2ObjectOpenHashMap<ResourceLocation, BakedGeoModel>()

	/**
	 * 服务端资源重载入口，在数据包重载时异步加载并缓存所有动画和模型数据。
	 * 使用 [GeckoLibCacheAccessor]（Mixin）调用 GeckoLib 的加载方法，
	 * 然后将结果合并到本地的 [ANIMATIONS] 和 [MODELS] 缓存中。
	 *
	 * @param stage 重载阶段屏障
	 * @param resourceManager 资源管理器
	 * @param preparationsProfiler 准备阶段性能分析器
	 * @param reloadProfiler 重载阶段性能分析器
	 * @param backgroundExecutor 后台执行器（用于加载）
	 * @param gameExecutor 游戏主线程执行器（用于缓存写入）
	 * @return 完成后的 Future
	 */
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
