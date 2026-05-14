package architecture.resonator_combat_framework.module.player_animation.mixin.gecko_lib;

import architecture.resonator_combat_framework.module.player_animation.util.AnimatableManagerControllerRegistrarUtil;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;

import java.util.Map;

@Mixin(AnimatableManager.class)
public abstract class AnimatableManagerMixin {

	@Mutable
	@Shadow
	@Final
	private Map<String, AnimationController<?>> animationControllers;

	@Inject(method = "<init>", at = @At(value = "RETURN", target = "Lsoftware/bernie/geckolib/animation/AnimatableManager$ControllerRegistrar;build()Lit/unimi/dsi/fastutil/objects/Object2ObjectArrayMap;"))
	private <T extends GeoAnimatable> void resonator_combat_framework$init1(
		GeoAnimatable animatable, CallbackInfo ci,
		@Local(name = "registrar") AnimatableManager.ControllerRegistrar registrar
	) {
		animationControllers = AnimatableManagerControllerRegistrarUtil.build(registrar);
	}
}