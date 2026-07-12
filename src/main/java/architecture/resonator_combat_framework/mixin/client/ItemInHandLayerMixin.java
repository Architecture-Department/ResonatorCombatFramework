/**
 * ItemInHandLayer Mixin —— 在手持物品渲染前应用动画骨骼变换。
 * 注入到 [ItemInHandLayer.renderArmWithItem] 中，使手持物品跟随动画骨骼位置。
 */
package architecture.resonator_combat_framework.mixin.client;

import architecture.resonator_combat_framework.animation.IAnimationProvider;
import architecture.resonator_combat_framework.animation.mapper.HumanoidEntityAnimationMapperProvider;
import architecture.resonator_combat_framework.animation.mapper.IEntityAnimationMapperProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", ordinal = 0))
	private void resonator_combat_framework$renderItem(
		LivingEntity livingEntity,
		ItemStack itemStack,
		ItemDisplayContext displayContext,
		HumanoidArm arm,
		PoseStack poseStack,
		MultiBufferSource buffer,
		int packedLight,
		CallbackInfo ci
	) {
		if (!(livingEntity instanceof IAnimationProvider player)) return;
		IEntityAnimationMapperProvider<?, ?> iAnimationMapper = player.resonator_combat_framework$getMapperProvider();
		if (!iAnimationMapper.isActive() || !(iAnimationMapper instanceof HumanoidEntityAnimationMapperProvider<?, ?> humanoidEntityAnimationMapper)) {
			return;
		}
		humanoidEntityAnimationMapper.applyItemTransform(arm == HumanoidArm.LEFT, poseStack);
	}
}

