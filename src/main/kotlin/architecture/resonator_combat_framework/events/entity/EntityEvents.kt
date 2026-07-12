package architecture.resonator_combat_framework.events.entity

import architecture.resonator_combat_framework.animation.IAnimationProvider
import architecture.resonator_combat_framework.animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent

/**
 * 实体 tick 事件 —— 驱动实体上的 MoLang 数据更新、动画控制器和状态持有者 tick。
 */
@EventBusSubscriber(modid = RcfUtil.ID)
object EntityEvents {
	@SubscribeEvent
	fun onTickPre(event: EntityTickEvent.Pre) {
		val entity = event.entity

		if (entity is LivingEntity) {
			val stateHolderOptional = entity.getExistingData(RcfAttachmentTypes.STATE_HOLDER)
			if (stateHolderOptional.isPresent) {
				val stateHolder = stateHolderOptional.get()
			}
		}

		entity.getExistingDataOrNull(RcfAttachmentTypes.MOLANG_DATA)?.apply {}
		if (entity is IAnimationProvider) {
			entity.getMapperProvider().tick()
		}
		entity.getExistingDataOrNull(RcfAttachmentTypes.STATE_HOLDER)?.apply {
			tick()
		}
	}
}
