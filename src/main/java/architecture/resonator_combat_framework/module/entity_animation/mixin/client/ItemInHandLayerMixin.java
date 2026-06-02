package architecture.resonator_combat_framework.module.entity_animation.mixin.client;

import architecture.resonator_combat_framework.module.entity_animation.mapper.PlayerAnimationMapper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
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

/**
 * 从 PlayerAnimationTransformer 读取 eyelib 虚拟骨骼 "right_item"/"left_item" 的动画数据，
 * 应用到 PoseStack，实现物品独立于手臂的动画。
 */
@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {

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
		if (!(livingEntity instanceof AbstractClientPlayer player)) return;
		PlayerAnimationMapper transformer = player.resonator_combat_framework$getAnimationTransformer();
		if (!transformer.isActive()) return;
		transformer.applyItemTransform(arm == HumanoidArm.LEFT, poseStack);
	}
}

