package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.combat.Action
import architecture.resonator_combat_framework.common.item_property.ItemProperty
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.neoforged.neoforge.registries.NewRegistryEvent
import net.neoforged.neoforge.registries.RegistryBuilder

/**
 * RCF 自定义注册表管理器 —— 管理所有通过 [NewRegistryEvent] 注册的自定义 Registry。
 * 提供统一的注册入口和便捷的键创建方法。
 */
object RcfRegistries {
	private val REGISTRIES = mutableListOf<Registry<*>>()

	@JvmField
	val ACTION_KEY: ResourceKey<Registry<Action>> = key("action")

	@JvmField
	val ACTION: MappedRegistry<Action> = register(ACTION_KEY) { it.sync(true) }

	@JvmField
	val ITEM_PROPERTY_KEY: ResourceKey<Registry<ItemProperty>> = key("item_property")

	@JvmField
	val ITEM_PROPERTY: MappedRegistry<ItemProperty> = register(ITEM_PROPERTY_KEY) { it.sync(true) }

	/** 将所有自定义注册表注册到 NeoForge 的 NewRegistryEvent */
	@JvmStatic
	internal fun register(event: NewRegistryEvent) {
		REGISTRIES.forEach { event.register(it) }
	}

	/** 创建并注册一个自定义 Registry，返回创建的 MappedRegistry */
	private fun <T> register(
		key: ResourceKey<Registry<T>>, function: (RegistryBuilder<T>) -> RegistryBuilder<T>
	): MappedRegistry<T> {
		val registry = function(RegistryBuilder(key)).create()
		REGISTRIES.add(registry)
		return registry as MappedRegistry<T>
	}

	/** 快捷创建以 mod ID 为命名空间的 RegistryKey */
	private fun <T> key(name: String): ResourceKey<Registry<T>> {
		return ResourceKey.createRegistryKey(RcfUtil.modRl(name))
	}
}