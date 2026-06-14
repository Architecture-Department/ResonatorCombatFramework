package architecture.resonator_combat_framework.events

import architecture.goldenboughs_lib.util.LibUtil
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent

@EventBusSubscriber(modid = LibUtil.ID)
object EntityEvetns {
	@SubscribeEvent
	fun onTickPre(event: EntityTickEvent.Pre) {
		val entity = event.entity
		entity.getExistingDataOrNull(RcfAttachmentTypes.MOLANG_DATA)?.apply {
			// TODO 补充
		}
	}
}
