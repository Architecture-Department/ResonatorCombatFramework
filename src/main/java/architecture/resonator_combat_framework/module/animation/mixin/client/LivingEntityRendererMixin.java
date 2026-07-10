/**
 * LivingEntityRenderer Mixin —— 在生物渲染前驱动动画控制器更新。
 * 注入到 [LivingEntityRenderer.render] 中，调用 [IEntityAnimationMapperProvider.tickAndRender]
 * 更新模型变换和过渡插值。
 */
package architecture.resonator_combat_framework.module.animation.mixin.client;

import architecture.resonator_combat_framework.module.animation.mapper.IEntityAnimationMapperProvider;
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
		//noinspection rawtypes
		IEntityAnimationMapperProvider transformer = player.resonator_combat_framework$getMapperProvider();
		//noinspection unchecked
		transformer.tickAndRender(playerModel, partialTicks, poseStack);
	}
}
