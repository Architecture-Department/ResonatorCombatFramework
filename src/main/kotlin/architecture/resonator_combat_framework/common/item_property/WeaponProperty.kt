package architecture.resonator_combat_framework.common.item_property

import architecture.resonator_combat_framework.common.payload.AttackPayload
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.entity_state_machine.combat.Action
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionSequence
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import java.util.function.Supplier

class WeaponProperty
@JvmOverloads
constructor(
	id: ResourceLocation,
	val actionSequence: Supplier<ActionSequence>,
	val longAction: Supplier<Action>? = null
) : ItemProperty(id) {
	override fun onUse(item: ItemStack, entity: LivingEntity, hand: InteractionHand, pressType: AttackPayload.PressType) {
	}

	override fun onAttack(
		item: ItemStack,
		entity: LivingEntity,
		hand: InteractionHand,
		pressType: AttackPayload.PressType
	) {
		val stateHolder = entity.getData(RcfAttachmentTypes.STATE_HOLDER)
		val actionController = stateHolder.actionController

		if (longAction != null && pressType == AttackPayload.PressType.LONG) {
			actionController.onChangedAction(longAction!!.get())
			return
		}

		val actionSequence = actionSequence.get()
		if (actionController.actionSequence?.id != actionSequence.id) {
			actionController.actionSequence = actionSequence
		}

		actionController.onNextAction()
	}
}
