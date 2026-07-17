package architecture.resonator_combat_framework.common.item_property

import architecture.goldenboughs_lib.api.AllOpen
import architecture.resonator_combat_framework.payload.tosc.AttackPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

/**
 * 物品属性 —— 定义物品持有 RCF 战斗能力的行为接口。
 *
 * 通过 [WeaponProperty] 等实现类，将攻击逻辑（短按/长按）绑定到物品上，
 * 通过 NeoForge Capability 系统附加到 ItemStack。
 *
 * @param id 属性唯一标识符
 */
@AllOpen
abstract class ItemProperty(
	val id: ResourceLocation
) {
	abstract fun onUse(item: ItemStack, entity: LivingEntity, hand: InteractionHand, pressType: AttackPayload.PressType)

	abstract fun onAttack(
		controllerId: ResourceLocation,
		item: ItemStack,
		entity: LivingEntity,
		hand: InteractionHand,
		pressType: AttackPayload.PressType
	)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ItemProperty) return false

		if (id != other.id) return false

		return true
	}

	override fun hashCode(): Int {
		return id.hashCode()
	}
}
