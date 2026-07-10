package architecture.resonator_combat_framework.common.item_property

import architecture.resonator_combat_framework.common.payload.AttackPayload
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.combat.Action
import architecture.resonator_combat_framework.module.combat.ActionSequence
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import java.util.function.Supplier

/**
 * 武器物品属性。
 *
 * 扩展 [ItemProperty]，为武器物品提供攻击动作调度逻辑。
 * 根据攻击类型（短按/长按）调度不同的动作序列或独立动作，
 * 并通过状态机的动作控制器驱动攻击动画与打击判定。
 *
 * @property actionSequence 默认攻击动作序列的提供者
 * @property longAction 长按攻击时的独立动作提供者（可选）
 */
class WeaponProperty
@JvmOverloads
constructor(
	id: ResourceLocation,
	val actionSequence: Supplier<ActionSequence>,
	val longAction: Supplier<Action>? = null
) : ItemProperty(id) {

	override fun onUse(
		item: ItemStack,
		entity: LivingEntity,
		hand: InteractionHand,
		pressType: AttackPayload.PressType
	) {
	}

	/**
	 * 攻击处理入口 —— 被 [AttackPayload] 从服务端/客户端的网络包处理中调用。
	 * 根据按压类型决定调度逻辑：长按触发独立动作（若有），短按在动作序列中推进到下一段。
	 */
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

		if (actionController.onNextAction()) {
			actionController.combatSpeedMultiplier = entity.getAttributeValue(Attributes.ATTACK_SPEED).toFloat()
		} else {
			actionController.combatSpeedMultiplier = 1f
		}
	}
}
