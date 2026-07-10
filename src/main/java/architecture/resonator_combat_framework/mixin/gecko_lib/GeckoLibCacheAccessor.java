/**
 * GeckoLib 缓存访问器 —— 通过 @Invoker 暴露 GeckoLibCache 的私有静态方法。
 * 用于在自定义资源加载流程中调用 GeckoLib 的动画/模型加载逻辑。
 */
package architecture.resonator_combat_framework.mixin.gecko_lib;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

@Mixin(GeckoLibCache.class)
public interface GeckoLibCacheAccessor {
	@Invoker
	static CompletableFuture<Void> callLoadAnimations(Executor backgroundExecutor, ResourceManager resourceManager, BiConsumer<ResourceLocation, BakedAnimations> elementConsumer) {
		throw new AssertionError();
	}

	@Invoker
	static CompletableFuture<Void> callLoadModels(Executor backgroundExecutor, ResourceManager resourceManager, BiConsumer<ResourceLocation, BakedGeoModel> elementConsumer) {
		throw new AssertionError();
	}
}
