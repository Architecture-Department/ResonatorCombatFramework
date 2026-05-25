package architecture.resonator_combat_framework.module.player_animation.mixin.eye_lib;

import architecture.resonator_combat_framework.module.player_animation.mixed.IBoneRenderInfoEntry;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.client.animation.bedrock.BrBoneAnimation;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import io.github.tt432.eyelib.molang.MolangScope;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrAnimationEntry.class)
public abstract class BrAnimationEntryMixin {
	@WrapOperation(method = "lambda$tickAnimation$34", at = @At(value = "INVOKE", target = "Lio/github/tt432/eyelib/client/animation/bedrock/BrBoneAnimation;lerpPosition(Lio/github/tt432/eyelib/molang/MolangScope;F)Lorg/joml/Vector3f;"))
	private static Vector3f resonator_combat_framework$tickAnimation(
		BrBoneAnimation instance,
		MolangScope scope,
		float currentTick,
		Operation<Vector3f> original,
		@Local(name = "renderInfoEntry") BoneRenderInfoEntry renderInfoEntry
	) {
		Vector3f pos = original.call(instance, scope, currentTick);
		IBoneRenderInfoEntry.of(renderInfoEntry).setRenderPositionEmpty(pos == null);
		return pos;
	}

	@WrapOperation(method = "lambda$tickAnimation$34", at = @At(value = "INVOKE", target = "Lio/github/tt432/eyelib/client/animation/bedrock/BrBoneAnimation;lerpRotation(Lio/github/tt432/eyelib/molang/MolangScope;F)Lorg/joml/Vector3f;"))
	private static Vector3f resonator_combat_framework$tickAnimation1(
		BrBoneAnimation instance,
		MolangScope scope,
		float currentTick,
		Operation<Vector3f> original,
		@Local(name = "renderInfoEntry") BoneRenderInfoEntry renderInfoEntry
	) {
		Vector3f rotation = original.call(instance, scope, currentTick);
		IBoneRenderInfoEntry.of(renderInfoEntry).setRenderRotationEmpty(rotation == null);
		return rotation;
	}

	@WrapOperation(method = "lambda$tickAnimation$34", at = @At(value = "INVOKE", target = "Lio/github/tt432/eyelib/client/animation/bedrock/BrBoneAnimation;lerpScale(Lio/github/tt432/eyelib/molang/MolangScope;F)Lorg/joml/Vector3f;"))
	private static Vector3f resonator_combat_framework$tickAnimation2(
		BrBoneAnimation instance,
		MolangScope scope,
		float currentTick,
		Operation<Vector3f> original,
		@Local(name = "renderInfoEntry") BoneRenderInfoEntry renderInfoEntry
	) {
		Vector3f scale = original.call(instance, scope, currentTick);
		IBoneRenderInfoEntry.of(renderInfoEntry).setRenderScalaEmpty(scale == null);
		return scale;
	}
}
