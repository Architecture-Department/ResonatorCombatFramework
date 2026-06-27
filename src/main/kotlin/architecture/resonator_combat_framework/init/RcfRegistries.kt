package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.neoforged.neoforge.registries.NewRegistryEvent
import net.neoforged.neoforge.registries.RegistryBuilder

object RcfRegistries {
	private val REGISTRIES = mutableListOf<Registry<*>>()

	@JvmStatic
	internal fun register(event: NewRegistryEvent) {
		REGISTRIES.forEach {
			event.register(it)
		}
	}

	private fun <T> register(
		key: ResourceKey<Registry<T>>, function: (RegistryBuilder<T>) -> RegistryBuilder<T>
	): MappedRegistry<T> {
		val registry = function(RegistryBuilder(key)).create()
		REGISTRIES.add(registry)
		return registry as MappedRegistry<T>
	}

	private fun <T> key(name: String): ResourceKey<Registry<T>> {
		return ResourceKey.createRegistryKey(RcfUtil.modRl(name))
	}
}