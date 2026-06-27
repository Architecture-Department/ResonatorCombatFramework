package architecture.resonator_combat_framework.events

import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.collision.CollisionSystem
import architecture.resonator_combat_framework.module.entity_animation.mixed.IAnimationProxyProvider
import architecture.resonator_combat_framework.module.entity_animation.mixed.IAnimationProxyProvider.Companion.getAnimationTransformer
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent

@EventBusSubscriber(modid = RcfUtil.ID)
object EntityEvents {
	@SubscribeEvent
	fun onTickPre(event: EntityTickEvent.Pre) {
		val entity = event.entity
		// 动画 tick（仅支持 IAnimationProxyProvider 的实体，当前仅为 Player）
		if (entity is IAnimationProxyProvider) {
			entity.getAnimationTransformer().tickAnimationManager()
		}
		entity.getExistingDataOrNull(RcfAttachmentTypes.MOLANG_DATA)?.apply {}
		entity.getExistingDataOrNull(RcfAttachmentTypes.STATE_HOLDER)?.apply {
			tick()
		}
		CollisionSystem.tick(event.entity)
	}
}
