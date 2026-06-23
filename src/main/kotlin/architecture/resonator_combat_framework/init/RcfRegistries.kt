package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.animation.Animation
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.neoforged.neoforge.registries.NewRegistryEvent
import net.neoforged.neoforge.registries.RegistryBuilder

object RcfRegistries {
	private val REGISTRIES = mutableListOf<Registry<*>>()

	@JvmField
	val ANIMATION_KEY: ResourceKey<Registry<Animation>> = key("ANIMATION")

	@JvmField
	val ANIMATION: Registry<Animation> = animations(ANIMATION_KEY) { it.sync(true) }

	private fun <T> animations(
		key: ResourceKey<Registry<T>>, function: (RegistryBuilder<T>) -> RegistryBuilder<T>
	): Registry<T> {
		val registry = function(RegistryBuilder(key)).create()
		REGISTRIES.add(registry)
		return registry
	}

	fun register(event: NewRegistryEvent) {
		REGISTRIES.forEach {
			event.register(it)
		}
	}

	private fun <T> key(name: String): ResourceKey<Registry<T>> {
		return ResourceKey.createRegistryKey(RcfUtil.modRl(name))
	}
}