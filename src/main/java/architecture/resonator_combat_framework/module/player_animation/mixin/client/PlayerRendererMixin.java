package architecture.resonator_combat_framework.module.player_animation.mixin.client;

import architecture.resonator_combat_framework.module.player_animation.RcfFirstPersonRender;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
	@Inject(method = "setModelProperties", at = @At("RETURN"))
	private void setModelProperties(
		AbstractClientPlayer clientPlayer,
		CallbackInfo ci,
		@Local PlayerModel<AbstractClientPlayer> playerModel
	) {
		if (RcfFirstPersonRender.isFirstPersonPass()) {
			playerModel.setAllVisible(false);
			playerModel.rightArm.visible = true;
			playerModel.leftArm.visible = true;
		}
	}
}
