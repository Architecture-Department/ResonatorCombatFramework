package architecture.resonator_combat_framework.mixin.gecko_lib;

import architecture.resonator_combat_framework.api.event.AddGeckoLibCachePathEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import software.bernie.geckolib.GeckoLibConstants;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.json.typeadapter.BakedAnimationsAdapter;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.CompoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Mixin(GeckoLibCache.class)
public abstract class GeckoLibCacheMixin {

	@Shadow
	private static <T> CompletableFuture<Void> loadResources(Executor executor, ResourceManager resourceManager, String path, Function<ResourceLocation, T> loader, BiConsumer<ResourceLocation, T> map) {
		return null;
	}

	@WrapOperation(method = "reload", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;allOf([Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;"))
	private static CompletableFuture<Void> resonator_combat_framework$reload(
		CompletableFuture<?>[] cfs,
		Operation<CompletableFuture<Void>> original,
		@Local(name = "backgroundExecutor") Executor backgroundExecutor,
		@Local(name = "resourceManager") ResourceManager resourceManager,
		@Local(name = "animations") Map<ResourceLocation, BakedAnimations> animations,
		@Local(name = "models") Map<ResourceLocation, BakedGeoModel> models
	) {
		List<CompletableFuture<?>> list = new ArrayList<>(List.of(cfs));
		AddGeckoLibCachePathEvent event = NeoForge.EVENT_BUS.post(new AddGeckoLibCachePathEvent());
		for (var path : event.getAnimationPaths()) {
			list.add(resonator_combat_framework$loadAnimations(path, backgroundExecutor, resourceManager, animations::put));
		}
		for (var path : event.getModelPaths()) {
			list.add(resonator_combat_framework$loadModels(path, backgroundExecutor, resourceManager, models::put));
		}
		return original.call((Object) list.toArray(CompletableFuture<?>[]::new));
	}

	@Unique
	private static CompletableFuture<Void> resonator_combat_framework$loadModels(String name, Executor backgroundExecutor, ResourceManager resourceManager, BiConsumer<ResourceLocation, BakedGeoModel> elementConsumer) {
		return loadResources(backgroundExecutor, resourceManager, name, resource -> {
			try {
				Model model = FileLoader.loadModelFile(resource, resourceManager);

				switch (model.formatVersion()) {
					case V_1_12_0 -> {
					}
					case V_1_14_0 ->
						GeckoLibConstants.LOGGER.warn("Unsupported geometry json version: 1.14.0 for model {}. This model may not appear as expected", resource);
					case V_1_21_0 ->
						GeckoLibConstants.LOGGER.warn("Unsupported geometry json version: 1.21.0 for model {}. Supported versions: 1.12.0. Remove any rotated face UVs and re-export the model to fix", resource);
					case null, default ->
						GeckoLibConstants.LOGGER.warn("Unsupported geometry json version for model {}. Supported versions: 1.12.0", resource);
				}

				return BakedModelFactory.getForNamespace(resource.getNamespace()).constructGeoModel(GeometryTree.fromModel(model));
			} catch (Exception ex) {
				throw GeckoLibConstants.exception(resource, "Error loading model file", ex);
			}
		}, elementConsumer);
	}

	@Unique
	private static CompletableFuture<Void> resonator_combat_framework$loadAnimations(String name, Executor backgroundExecutor, ResourceManager resourceManager, BiConsumer<ResourceLocation, BakedAnimations> elementConsumer) {
		return CompletableFuture.runAsync(() -> BakedAnimationsAdapter.COMPRESSION_CACHE = new ConcurrentHashMap<>(), backgroundExecutor)
			.thenRunAsync(() ->
				loadResources(backgroundExecutor, resourceManager, name, resource -> {
					try {
						return FileLoader.loadAnimationsFile(resource, resourceManager);
					} catch (CompoundException ex) {
						ex.withMessage(resource.toString() + ": Error loading animation file").printStackTrace();
						return new BakedAnimations(new Object2ObjectOpenHashMap<>());
					} catch (Exception ex) {
						throw GeckoLibConstants.exception(resource, "Error loading animation file", ex);
					}
				}, elementConsumer))
			.thenRunAsync(() -> BakedAnimationsAdapter.COMPRESSION_CACHE = null);
	}
}
