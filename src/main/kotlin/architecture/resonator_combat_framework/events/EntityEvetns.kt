package architecture.resonator_combat_framework.events

import architecture.goldenboughs_lib.core.Lib
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.EntityTickEvent

@EventBusSubscriber(modid = Lib.ID)
object EntityEvetns {
	@SubscribeEvent
	fun entityPre(pre: EntityTickEvent.Pre) {
		val entity = pre.entity
	}
}
