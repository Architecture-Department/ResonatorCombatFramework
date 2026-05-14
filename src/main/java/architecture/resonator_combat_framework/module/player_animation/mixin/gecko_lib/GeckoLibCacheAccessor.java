package architecture.resonator_combat_framework.module.player_animation.mixin.gecko_lib;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.util.Map;
import java.util.Set;

@Mixin(GeckoLibCache.class)
public interface GeckoLibCacheAccessor {

	@Accessor
	static Set<String> getEXCLUDED_NAMESPACES() {
		throw new AssertionError();
	}

	@Accessor
	static Map<ResourceLocation, BakedAnimations> getANIMATIONS() {
		throw new AssertionError();
	}

	@Accessor
	static void setANIMATIONS(Map<ResourceLocation, BakedAnimations> map) {
		throw new AssertionError();
	}

	@Accessor
	static Map<ResourceLocation, BakedGeoModel> getMODELS() {
		throw new AssertionError();
	}

	@Accessor
	static void setMODELS(Map<ResourceLocation, BakedGeoModel> map) {
		throw new AssertionError();
	}
}
