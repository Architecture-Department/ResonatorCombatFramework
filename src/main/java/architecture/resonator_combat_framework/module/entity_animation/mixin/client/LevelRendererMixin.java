package architecture.resonator_combat_framework.module.entity_animation.mixin.client;

import architecture.resonator_combat_framework.module.entity_animation.IProxyAnimationProvider;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Unique
	private boolean resonator_combat_framework$defaultCameraState = false;

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z"))
	private void fakeThirdPersonMode(
		DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera,
		GameRenderer gameRenderer, LightTexture lightTexture,
		Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci
	) {
		resonator_combat_framework$defaultCameraState = camera.isDetached();
		if (!camera.isDetached() && camera.getEntity() instanceof IProxyAnimationProvider rcf
			&& rcf.resonator_combat_framework$getMapperProvider().isActive()) {
			((CameraAccessor) camera).setDetached(true);
		}
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z", shift = At.Shift.AFTER))
	private void resetThirdPerson(
		DeltaTracker deltaTracker, boolean bl, Camera camera,
		GameRenderer gameRenderer, LightTexture lightTexture,
		Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci
	) {
		((CameraAccessor) camera).setDetached(resonator_combat_framework$defaultCameraState);
	}
}

