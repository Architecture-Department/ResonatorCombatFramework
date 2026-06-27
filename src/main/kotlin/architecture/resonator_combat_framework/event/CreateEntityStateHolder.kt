package architecture.resonator_combat_framework.event

import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.Event
import java.util.function.Supplier

class CreateEntityStateHolder : Event() {
	companion object {
		private lateinit var MAP_CACHE: HashMap<EntityType<*>, (LivingEntity) -> EntityStateHolder<*>>
		private val MAP = hashMapOf<Supplier<EntityType<*>>, (entity: LivingEntity) -> EntityStateHolder<*>>()

		@JvmStatic
		fun getAll(): Map<EntityType<*>, (entity: LivingEntity) -> EntityStateHolder<*>> {
			if (!::MAP_CACHE.isInitialized) {
				MAP_CACHE = hashMapOf()
				MAP.forEach { MAP_CACHE[it.key.get()] = it.value }
			}
			return MAP_CACHE
		}
	}

	fun register(entityType: Supplier<EntityType<*>>, function: (entity: LivingEntity) -> EntityStateHolder<*>) {
		MAP[entityType] = function
	}
}