package architecture.resonator_combat_framework.animation_controller

import architecture.resonator_combat_framework.config.RcfConfig
import architecture.resonator_combat_framework.module.player_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.player_animation.controller.BedrockAnimationController
import architecture.resonator_combat_framework.module.player_animation.mapper.LivingEntityAnimationMapper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class ActionAnimationController(
	id: ResourceLocation,
	isClient: Boolean
) : BedrockAnimationController(id, isClient) {
	var mainHandItem: ItemStack? = null
	var offhandItem: ItemStack? = null
	override fun tick(animationMapper: IAnimationMapper) {
		super.tick(animationMapper)
		if (animationMapper !is LivingEntityAnimationMapper<*, *>) return
		val entity = animationMapper.entity

		if (entity.level().isClientSide) {

			if (entity is Player) {
				if (RcfConfig.CLIENT.itemSwitchingAnimation.get()) {
					val mainHandItem = entity.mainHandItem
					val offhandItem = entity.offhandItem
					if (mainHandItem != this.mainHandItem) {
						this.mainHandItem = entity.mainHandItem
						if (mainHandItem != ItemStack.EMPTY) {
							trigger(getSwitchingAnimId(entity.mainArm == HumanoidArm.RIGHT))
						}
					} else if (offhandItem != this.offhandItem) {
						this.offhandItem = entity.offhandItem
						if (offhandItem != ItemStack.EMPTY) {
							trigger(getSwitchingAnimId(entity.mainArm != HumanoidArm.RIGHT))
						}
					}
				}
			}
		}
	}

	private fun getSwitchingAnimId(isRight: Boolean): String = "player." +
		if (isRight) "item_switching_right"
		else "item_switching_left"
}