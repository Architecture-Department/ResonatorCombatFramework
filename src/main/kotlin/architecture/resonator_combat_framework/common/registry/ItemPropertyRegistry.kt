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

/**
 * 物品属性注册表。
 *
 * 继承 [SimplePreparableReloadListener] 在数据包重载时重新收集所有 [ItemProperty] 注册项。
 * 通过 [ItemPropertyRegistryEvent] 事件从各个模组收集属性定义，
 * 并完成 [LazySupplier] 的初始化。
 */
@AllOpe
object ItemPropertyRegistry : SimplePreparableReloadListener<Map<ResourceLocation, LazySupplier<ItemProperty>>>() {
	private val itemPropertys = mutableMapOf<ResourceLocation, LazySupplier<ItemProperty>>()

	/**
	 * 根据 ID 获取物品属性。
	 *
	 * @param id 属性 ID
	 * @return 属性的延迟加载器，若不存在则返回 null
	 */
	@JvmStatic
	fun get(id: ResourceLocation): LazySupplier<ItemProperty>? = itemPropertys[id]

	/**
	 * 检查指定 ID 的物品属性是否已注册。
	 *
	 * @param id 属性 ID
	 * @return 是否已注册
	 */
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
