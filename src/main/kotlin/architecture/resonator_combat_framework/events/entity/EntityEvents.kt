package architecture.resonator_combat_framework.events.entity

import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.collision.CollisionSystem
import architecture.resonator_combat_framework.module.animation.IAnimationProvider
import architecture.resonator_combat_framework.module.animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent

@EventBusSubscriber(modid = RcfUtil.ID)
object EntityEvents {
	@SubscribeEvent
	fun onTickPre(event: EntityTickEvent.Pre) {
		val entity = event.entity

		if (entity is LivingEntity) {
			val stateHolderOptional = entity.getExistingData(RcfAttachmentTypes.STATE_HOLDER)
			if (stateHolderOptional.isPresent) {
				val stateHolder = stateHolderOptional.get()

//				if (!stateHolder.getState(EntityStateHolder.CAN_MOVE) || stateHolder.getFloatState(EntityStateHolder.SPEED_MODIFIER) <= 0.01f) {
//					entity.xxa = 0f
//					entity.zza = 0f
//				}
//
//				// 视角限制
//				if (!stateHolder.getState(EntityStateHolder.CAN_LOOK_AROUND)) {
//					// 阻止玩家视角变化
//					// 简单地保持当前旋转不变
//				}
			}
		}

		entity.getExistingDataOrNull(RcfAttachmentTypes.MOLANG_DATA)?.apply {}
		if (entity is IAnimationProvider) {
			entity.getMapperProvider().tick()
		}
		entity.getExistingDataOrNull(RcfAttachmentTypes.STATE_HOLDER)?.apply {
			tick()
		}
		CollisionSystem.tick(event.entity)
	}
}
