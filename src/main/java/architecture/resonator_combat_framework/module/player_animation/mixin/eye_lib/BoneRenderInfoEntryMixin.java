package architecture.resonator_combat_framework.module.player_animation.mixin.eye_lib;

import architecture.resonator_combat_framework.module.player_animation.mixed.IBoneRenderInfoEntry;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BoneRenderInfoEntry.class)
public abstract class BoneRenderInfoEntryMixin implements IBoneRenderInfoEntry {
	@Unique
	private boolean resonator_combat_framework$renderScalaEmpty = false;
	@Unique
	private boolean resonator_combat_framework$renderPositionEmpty = false;
	@Unique
	private boolean resonator_combat_framework$renderRotationEmpty = false;

	@Override
	public boolean resonator_combat_framework$isRenderScalaEmpty() {
		return resonator_combat_framework$renderScalaEmpty;
	}

	@Override
	public boolean resonator_combat_framework$isRenderPositionEmpty() {
		return resonator_combat_framework$renderPositionEmpty;
	}

	@Override
	public boolean resonator_combat_framework$isRenderRotationEmpty() {
		return resonator_combat_framework$renderRotationEmpty;
	}

	@Override
	public void resonator_combat_framework$setRenderScalaEmpty(boolean value) {
		resonator_combat_framework$renderScalaEmpty = value;
	}

	@Override
	public void resonator_combat_framework$setRenderPositionEmpty(boolean value) {
		resonator_combat_framework$renderPositionEmpty = value;
	}

	@Override
	public void resonator_combat_framework$setRenderRotationEmpty(boolean value) {
		resonator_combat_framework$renderRotationEmpty = value;
	}

	@Inject(method = "resetRenderInfo", at = @At("RETURN"))
	private void resetRenderInfo(CallbackInfo ci) {
		resonator_combat_framework$renderScalaEmpty = true;
		resonator_combat_framework$renderPositionEmpty = true;
		resonator_combat_framework$renderRotationEmpty = true;
	}
}
