package architecture.resonator_combat_framework.event

import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.common.item_property.ItemProperty
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.Event

class ItemPropertyRegistryEvent : Event() {
	private val map = linkedMapOf<ResourceLocation, LazySupplier<ItemProperty>>()

	fun <T : ItemProperty> register(
		id: ResourceLocation, function: (id: ResourceLocation) -> T
	): LazySupplier<T> {
		@Suppress("RemoveExplicitTypeArguments") val supplier = LazySupplier<T> { function(id) }
		@Suppress("UNCHECKED_CAST")
		map[id] = supplier as LazySupplier<ItemProperty>
		return supplier
	}

	fun getAll(): Map<ResourceLocation, LazySupplier<ItemProperty>> = map
}