package architecture.resonator_combat_framework.module.entity_animation.mixin.client;

import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapperProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 在 vanilla setupAnim() 之后、renderToBuffer() 之前注入
// 参考 TheElixir 模式：读 ModelPart 初始值 → 动画偏移 → 写回
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {
	@Shadow
	protected EntityModel<T> model;

	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getOverlayCoords(Lnet/minecraft/world/entity/LivingEntity;F)I"))
	public void render(
		T entity,
		float entityYaw,
		float partialTicks,
		PoseStack poseStack,
		MultiBufferSource buffer,
		int packedLight,
		CallbackInfo ci
	) {
		if (!(entity instanceof Player player) || !(model instanceof PlayerModel<?> playerModel)) {
			return;
		}
		IEntityAnimationMapperProvider transformer = player.resonator_combat_framework$getAnimationTransformer();
		transformer.tickAndRender(playerModel, partialTicks, poseStack);
	}
}
