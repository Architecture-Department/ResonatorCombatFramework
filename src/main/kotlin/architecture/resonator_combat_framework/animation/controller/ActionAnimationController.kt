package architecture.resonator_combat_framework.animation.controller

import architecture.resonator_combat_framework.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.animation.mapper.IEntityAnimationMapperProvider
import architecture.resonator_combat_framework.animation.mapper.LivingEntityAnimationMapperProvider
import architecture.resonator_combat_framework.combat.AttackAnimationAction
import architecture.resonator_combat_framework.config.RcfConfig
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * 动作动画控制器。
 *
 * 扩展 [AnimationController]，在客户端每 tick 检测玩家主手/副手的物品变化，
 * 并在发生切换时自动触发对应的物品切换动画。
 *
 * @param T 受控实体的类型
 * @property mainHandItem 缓存的主手物品，用于检测变化
 * @property offhandItem 缓存的副手物品，用于检测变化
 */
class ActionAnimationController<T : Entity>(
	manager: AnimationControllerManager<T>,
	id: ResourceLocation,
	isClient: Boolean
) : AnimationController<T>(manager, id, isClient) {
	var mainHandItem: ItemStack? = null
	var offhandItem: ItemStack? = null

	/**
	 * 每 tick 检测玩家主手/副手物品变化，并在发生切换时触发对应的切换动画。
	 * 仅在客户端、玩家实体且配置启用了物品切换动画时生效。
	 */
	override fun tickHandler(manager: IEntityAnimationMapperProvider<T, *>) {
		super.tickHandler(manager)
		if (manager !is LivingEntityAnimationMapperProvider<*, *>) return
		val entity = manager.holder

		if (!entity.level().isClientSide) return
		if (entity !is Player) return
		if (!RcfConfig.CLIENT.itemSwitchingAnimation.get()) return
		val stateHolder = entity.getExistingDataOrNull(RcfAttachmentTypes.STATE_HOLDER)

		val mainHandItem = entity.mainHandItem
		val offhandItem = entity.offhandItem
		if (!ItemStack.isSameItem(mainHandItem, this.mainHandItem ?: ItemStack.EMPTY)) {
			this.mainHandItem = entity.mainHandItem
			if (mainHandItem != ItemStack.EMPTY) {
				if (stateHolder == null || stateHolder.actionController.action !is AttackAnimationAction) {
					trigger(getSwitchingAnimId(entity.mainArm == HumanoidArm.RIGHT))
				}
			}
		}

		if (!ItemStack.isSameItem(offhandItem, this.offhandItem ?: ItemStack.EMPTY)) {
			this.offhandItem = entity.offhandItem
			if (offhandItem != ItemStack.EMPTY) {
				if (stateHolder == null || stateHolder.actionController.action !is AttackAnimationAction) {
					trigger(getSwitchingAnimId(entity.mainArm != HumanoidArm.RIGHT))
				}
			}
		}
	}

	/**
	 * 获取指定手的物品切换动画 ID。
	 *
	 * @param isRight 是否为主手（右手）
	 * @return 对应的切换动画资源位置
	 */
	private fun getSwitchingAnimId(isRight: Boolean): ResourceLocation =
		RcfUtil.modRl("player/" + if (isRight) "item_switching_right" else "item_switching_left")
}