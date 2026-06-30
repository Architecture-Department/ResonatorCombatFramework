package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.common.item_property.ItemProperty
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.world.item.Item
import net.minecraft.world.level.ItemLike
import net.neoforged.neoforge.capabilities.ItemCapability
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import java.util.function.Supplier

/**
 * RCF 能力键定义。
 * 使用 NeoForge 的 [ItemCapability] 系统将 [ItemProperty] 附加到物品上。
 */
object RcfCapabilitys {
	/** 物品战斗能力键：用于附加 [ItemProperty]（如 [architecture.resonator_combat_framework.common.item_property.WeaponProperty]）到 [net.minecraft.world.item.ItemStack] */
	@JvmField
	val ITEM_ABILITY: ItemCapability<ItemProperty, Void?> = ItemCapability.createVoid(
		RcfUtil.modRl("item_ability"),
		ItemProperty::class.java
	)

	fun <T : ItemProperty, I : Item> RegisterCapabilitiesEvent.registerItemAbility(
		property: Supplier<T?>,
		vararg item: Supplier<I>
	) {
		registerItem(
			ITEM_ABILITY,
			{ _, _ -> property.get() },
			*item.map { it.get() as ItemLike }.toTypedArray()
		)
	}
}
