package architecture.resonator_combat_framework.module.entity_animation.controller

import architecture.resonator_combat_framework.config.RcfConfig
import architecture.resonator_combat_framework.module.entity_animation.mapper.IEntityAnimationMapper
import architecture.resonator_combat_framework.module.entity_animation.mapper.LivingEntityAnimationMapper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class ActionAnimationController<T : Entity>(
	manager: AnimationControllerManager<T>,
	id: ResourceLocation,
	isClient: Boolean
) : BedrockAnimationController<T>(manager, id, isClient) {
	var mainHandItem: ItemStack? = null
	var offhandItem: ItemStack? = null

	/** 每 tick 检测物品变化，触发切换动画 */
	override fun tickHandler(animationMapper: IEntityAnimationMapper<T, *>) {
		super.tickHandler(animationMapper)
		if (animationMapper !is LivingEntityAnimationMapper<*, *>) return
		val entity = animationMapper.holder

		if (!entity.level().isClientSide) return
		if (entity !is Player) return
		if (!RcfConfig.CLIENT.itemSwitchingAnimation.get()) return

		val mainHandItem = entity.mainHandItem
		val offhandItem = entity.offhandItem
		if (mainHandItem != this.mainHandItem) {
			this.mainHandItem = entity.mainHandItem
			if (mainHandItem != ItemStack.EMPTY) {
				trigger(getSwitchingAnimId(entity.mainArm == HumanoidArm.RIGHT))
			}
		}

		if (offhandItem != this.offhandItem) {
			this.offhandItem = entity.offhandItem
			if (offhandItem != ItemStack.EMPTY) {
				trigger(getSwitchingAnimId(entity.mainArm != HumanoidArm.RIGHT))
			}
		}
	}

	/** 获取切换动画 ID */
	private fun getSwitchingAnimId(isRight: Boolean): String =
		"player." + if (isRight) "item_switching_right" else "item_switching_left"
}