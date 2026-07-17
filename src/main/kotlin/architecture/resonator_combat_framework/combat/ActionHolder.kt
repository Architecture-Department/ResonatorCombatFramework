package architecture.resonator_combat_framework.combat

import architecture.goldenboughs_lib.api.AllOpen
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

@AllOpen
class ActionHolder<T : LivingEntity>(
	val entity: T
) {
	companion object {
		@JvmStatic
		fun <T : LivingEntity> of(entity: T): ActionHolder<T> {
			return entity.getData(RcfAttachmentTypes.ACTION_HOLDER) as ActionHolder<T>
		}

		@JvmStatic
		fun has(entity: LivingEntity): Boolean {
			return entity.hasData(RcfAttachmentTypes.ACTION_HOLDER)
		}
	}

	private val nameMap = mutableMapOf<ResourceLocation, ActionController>()

	fun add(id: ResourceLocation, controller: ActionController): ActionController? {
		return nameMap.put(id, controller)
	}

	fun get(id: ResourceLocation): ActionController? {
		return nameMap[id]
	}

	fun getCreate(id: ResourceLocation): ActionController {
		return nameMap.getOrPut(id) { ActionController(id, entity) }
	}

	fun getAll(): Map<ResourceLocation, ActionController> {
		return nameMap
	}

	fun remove(name: ResourceLocation) {
		val ctrl = nameMap.remove(name) ?: return
		ctrl.actionSequence = null
		ctrl.onActionForcedEnd()
	}

	fun tick() {
		for (value in nameMap.values) {
			value.tick()
		}
	}
}