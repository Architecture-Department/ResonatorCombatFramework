package architecture.resonator_combat_framework.events

import architecture.goldenboughs_lib.core.LibConstants
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent

@EventBusSubscriber(modid = LibConstants.ID)
object EntityEvetns {
	@SubscribeEvent
	fun entityPre(pre: EntityTickEvent.Pre) {
		val entity = pre.entity
	}
}
