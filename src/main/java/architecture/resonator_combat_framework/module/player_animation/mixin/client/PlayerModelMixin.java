package architecture.resonator_combat_framework.module.player_animation.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
	@Shadow
	@Final
	public ModelPart jacket;

	@Shadow
	@Final
	public ModelPart rightSleeve;

	@Shadow
	@Final
	public ModelPart leftSleeve;

	@Shadow
	@Final
	public ModelPart rightPants;

	@Shadow
	@Final
	public ModelPart leftPants;

	public PlayerModelMixin(ModelPart root) {
		super(root);
	}

	@Unique
	private void resonator_combat_framework$setToInitialPose() {
		this.head.resetPose();
		this.body.resetPose();
		this.rightArm.resetPose();
		this.leftArm.resetPose();
		this.rightLeg.resetPose();
		this.leftLeg.resetPose();

		this.hat.resetPose();
		this.jacket.resetPose();
		this.rightSleeve.resetPose();
		this.leftSleeve.resetPose();
		this.rightPants.resetPose();
		this.leftPants.resetPose();
	}

	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", at = @At(value = "HEAD"))
	private void setDefaultBeforeRender(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
		resonator_combat_framework$setToInitialPose();
	}
}
