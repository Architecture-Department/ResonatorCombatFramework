package architecture.resonator_combat_framework.events.entity

import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent

/**
 * 生物事件 —— 处理装备变化、物品交换等生物专属事件。
 * 当前装备切换逻辑已注释（由动作系统内部处理），保留钩子以备用。
 */
@EventBusSubscriber(modid = RcfUtil.ID)
object LivingEntityEvents {
	/**
	 * 装备变化事件（主手物品切换/丢弃/交换）。
	 * 若当前禁止切物品（CAN_SWITCH_ITEM = false），由动作系统处理停止。
	 */
	@SubscribeEvent
	fun onEquipmentChange(event: LivingEquipmentChangeEvent) {
		val livingEntity = event.entity
		val equipmentSlot = event.slot

		val stateHolderOptional = livingEntity.getExistingData(RcfAttachmentTypes.STATE_HOLDER)
		if (stateHolderOptional.isPresent) {
			val stateHolder = stateHolderOptional.get()
//			if (!stateHolder.getState(EntityStateHolder.CAN_SWITCH_ITEM)) {
//				if (equipmentSlot != EquipmentSlot.MAINHAND && equipmentSlot != EquipmentSlot.OFFHAND) {
//					stateHolder.actionController.onActionForcedEnd()
//				}
//			}
		}
	}

	@SubscribeEvent
	fun onLivingSwapItemsHands(event: LivingSwapItemsEvent.Hands) {
		val livingEntity = event.entity

		val stateHolderOptional = livingEntity.getExistingData(RcfAttachmentTypes.STATE_HOLDER)
		if (stateHolderOptional.isPresent) {
			val stateHolder = stateHolderOptional.get()
//			if (!stateHolder.getState(EntityStateHolder.CAN_SWITCH_ITEM)) {
//				event.isCanceled = true
//				return
//			}
		}
	}
}