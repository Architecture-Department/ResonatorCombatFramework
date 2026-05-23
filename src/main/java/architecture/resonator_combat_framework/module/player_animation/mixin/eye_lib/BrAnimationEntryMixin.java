package architecture.resonator_combat_framework.module.player_animation.mixin.eye_lib;

import architecture.resonator_combat_framework.module.player_animation.mixed.IBoneRenderInfoEntry;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrAnimationEntry.class)
public abstract class BrAnimationEntryMixin {
	@Inject(method = "lambda$tickAnimation$34", at = @At(value = "INVOKE", target = "Lio/github/tt432/eyelib/client/render/bone/BoneRenderInfoEntry;getRenderPosition()Lorg/joml/Vector3f;"))
	private static void resonator_combat_framework$tickAnimation(
		CallbackInfo ci,
		@Local(name = "renderInfoEntry") BoneRenderInfoEntry renderInfoEntry
	) {
		IBoneRenderInfoEntry.of(renderInfoEntry).setRenderPositionEmpty(false);
	}

	@Inject(method = "lambda$tickAnimation$34", at = @At(value = "INVOKE", shift = At.Shift.BY, by = 2, target = "Lio/github/tt432/eyelib/client/animation/bedrock/BrBoneAnimation;lerpPosition(Lio/github/tt432/eyelib/molang/MolangScope;F)Lorg/joml/Vector3f;"))
	private static void resonator_combat_framework$tickAnimation1(
		CallbackInfo ci,
		@Local(name = "renderInfoEntry") BoneRenderInfoEntry renderInfoEntry,
		@Local(name = "pos") Vector3f pos
	) {
		if (pos == null) {
			IBoneRenderInfoEntry.of(renderInfoEntry).setRenderPositionEmpty(true);
		}
	}

	@Inject(method = "lambda$tickAnimation$34", at = @At(value = "INVOKE", target = "Lio/github/tt432/eyelib/client/render/bone/BoneRenderInfoEntry;getRenderRotation()Lorg/joml/Vector3f;"))
	private static void resonator_combat_framework$tickAnimation2(
		CallbackInfo ci,
		@Local(name = "renderInfoEntry") BoneRenderInfoEntry renderInfoEntry
	) {
		IBoneRenderInfoEntry.of(renderInfoEntry).setRenderRotationEmpty(false);
	}

	@Inject(method = "lambda$tickAnimation$34", at = @At(value = "INVOKE", shift = At.Shift.BY, by = 2, target = "Lio/github/tt432/eyelib/client/animation/bedrock/BrBoneAnimation;lerpRotation(Lio/github/tt432/eyelib/molang/MolangScope;F)Lorg/joml/Vector3f;"))
	private static void resonator_combat_framework$tickAnimation3(
		CallbackInfo ci,
		@Local(name = "renderInfoEntry") BoneRenderInfoEntry renderInfoEntry,
		@Local(name = "rotation") Vector3f rotation
	) {
		if (rotation == null) {
			IBoneRenderInfoEntry.of(renderInfoEntry).setRenderRotationEmpty(true);
		}
	}

	@Inject(method = "lambda$tickAnimation$34", at = @At(value = "INVOKE", target = "Lio/github/tt432/eyelib/client/render/bone/BoneRenderInfoEntry;getRenderScala()Lorg/joml/Vector3f;"))
	private static void resonator_combat_framework$tickAnimation4(
		CallbackInfo ci,
		@Local(name = "renderInfoEntry") BoneRenderInfoEntry renderInfoEntry
	) {
		IBoneRenderInfoEntry.of(renderInfoEntry).setRenderScalaEmpty(false);
	}

	@Inject(method = "lambda$tickAnimation$34", at = @At(value = "INVOKE", shift = At.Shift.BY, by = 2, target = "Lio/github/tt432/eyelib/client/animation/bedrock/BrBoneAnimation;lerpScale(Lio/github/tt432/eyelib/molang/MolangScope;F)Lorg/joml/Vector3f;"))
	private static void resonator_combat_framework$tickAnimation5(
		CallbackInfo ci,
		@Local(name = "renderInfoEntry") BoneRenderInfoEntry renderInfoEntry,
		@Local(name = "scale") Vector3f scale
	) {
		if (scale == null) {
			IBoneRenderInfoEntry.of(renderInfoEntry).setRenderScalaEmpty(true);
		}
	}
}
