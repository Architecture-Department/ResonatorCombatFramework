package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationDef
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.Event

/**
 * [AnimationDef]注册事件
 */
class AnimationDefRegistryEvent : Event() {
	private val map = linkedMapOf<ResourceLocation, LazySupplier<AnimationDef>>()

	fun <T : AnimationDef> register(
		id: ResourceLocation,
		function: (id: ResourceLocation) -> T
	): LazySupplier<T> {
		@Suppress("RemoveExplicitTypeArguments") val supplier = LazySupplier<T> { function(id) }
		@Suppress("UNCHECKED_CAST")
		map[id] = supplier as LazySupplier<AnimationDef>
		return supplier
	}

	fun getAll(): Map<ResourceLocation, LazySupplier<AnimationDef>> = map
}