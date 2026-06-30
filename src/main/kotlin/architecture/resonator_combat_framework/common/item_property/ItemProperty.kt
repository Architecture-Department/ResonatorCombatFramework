package architecture.resonator_combat_framework.common.item_property

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.common.payload.AttackPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

@AllOpe
abstract class ItemProperty(
	 val id: ResourceLocation
) {
	abstract fun onUse(item: ItemStack, entity: LivingEntity, hand: InteractionHand, pressType: AttackPayload.PressType)

	abstract fun onAttack(
		item: ItemStack,
		entity: LivingEntity,
		hand: InteractionHand,
		pressType: AttackPayload.PressType
	)
}
