package architecture.resonator_combat_framework.events

import architecture.resonator_combat_framework.core.RcfConstants
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.AddReloadListenerEvent

@EventBusSubscriber(modid = RcfConstants.ID)
object GameEvents {
	@SubscribeEvent
	fun onAddReloadListener(event: AddReloadListenerEvent) {

	}
}