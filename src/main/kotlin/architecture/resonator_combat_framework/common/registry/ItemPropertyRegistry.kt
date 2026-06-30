package architecture.resonator_combat_framework.common.registry

import architecture.goldenboughs_lib.api.AllOpe
import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.common.item_property.ItemProperty
import architecture.resonator_combat_framework.event.ItemPropertyRegistryEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

@AllOpe
object ItemPropertyRegistry : SimplePreparableReloadListener<Map<ResourceLocation, LazySupplier<ItemProperty>>>() {
	private val itemPropertys = mutableMapOf<ResourceLocation, LazySupplier<ItemProperty>>()

	@JvmStatic
	fun get(id: ResourceLocation): LazySupplier<ItemProperty>? = itemPropertys[id]

	@JvmStatic
	fun has(id: ResourceLocation): Boolean = itemPropertys.containsKey(id)

	override fun prepare(
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	): Map<ResourceLocation, LazySupplier<ItemProperty>> {
		return FORGE_BUS.post(ItemPropertyRegistryEvent()).getAll()
	}

	override fun apply(
		map: Map<ResourceLocation, LazySupplier<ItemProperty>>,
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	) {
		itemPropertys.clear()
		itemPropertys.putAll(map)
		itemPropertys.forEach {
			it.value.init()
		}
	}
}