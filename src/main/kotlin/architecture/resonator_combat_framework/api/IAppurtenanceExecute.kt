package architecture.resonator_combat_framework.api

import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack

interface IAppurtenanceExecute {
	fun add(entity: Entity, itemStack: ItemStack, map: Map<String, *>)

	fun add(entity: Entity, itemStack: ItemStack) {
		add(entity, itemStack, mapOf<String, Any>())
	}

	fun remove(entity: Entity, itemStack: ItemStack, map: Map<String, *>)

	fun remove(entity: Entity, itemStack: ItemStack) {
		remove(entity, itemStack, mapOf<String, Any>())
	}
}