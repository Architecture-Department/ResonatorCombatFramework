package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.Event

/**
 * [StaticAnimation]注册事件
 */
class StaticAnimationRegistryEvent : Event() {
	private val map = linkedMapOf<ResourceLocation, LazySupplier<StaticAnimation>>()

	fun <T : StaticAnimation> register(
		id: ResourceLocation, function: (id: ResourceLocation) -> T
	): LazySupplier<T> {
		@Suppress("RemoveExplicitTypeArguments") val supplier = LazySupplier<T> { function(id) }
		@Suppress("UNCHECKED_CAST")
		map[id] = supplier as LazySupplier<StaticAnimation>
		return supplier
	}

	fun getAll(): Map<ResourceLocation, LazySupplier<StaticAnimation>> = map
}