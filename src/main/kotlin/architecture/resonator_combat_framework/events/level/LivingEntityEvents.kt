package architecture.resonator_combat_framework.events.level

import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.world.entity.EquipmentSlot
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent

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
			if (!stateHolder.getState(EntityStateHolder.CAN_SWITCH_ITEM)) {
				if (equipmentSlot != EquipmentSlot.MAINHAND && equipmentSlot != EquipmentSlot.OFFHAND) {
					stateHolder.actionController.onActionForcedEnd()
				}
			}
		}
	}

	@SubscribeEvent
	fun onLivingSwapItemsHands(event: LivingSwapItemsEvent.Hands) {

	}
}