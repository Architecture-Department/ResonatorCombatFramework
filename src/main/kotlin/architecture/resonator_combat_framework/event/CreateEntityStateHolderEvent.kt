package architecture.resonator_combat_framework.event

import architecture.resonator_combat_framework.state_machine.holder.EntityStateHolder
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.Event
import java.util.function.Supplier

/**
 * 实体状态持有者创建事件 —— 在 [RcfAttachmentTypes.STATE_HOLDER] 首次被访问时触发。
 *
 * 外部模块可通过 [register] 方法为特定实体类型注册自定义的 [EntityStateHolder] 工厂，
 * 覆盖默认的 PlayerStateHolder / MobStateHolder / EntityStateHolder 策略。
 */
class CreateEntityStateHolderEvent : Event() {
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