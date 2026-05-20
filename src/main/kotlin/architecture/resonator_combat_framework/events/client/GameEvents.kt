package architecture.resonator_combat_framework.events.client

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.core.PlayerAnimationSetup
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent

@EventBusSubscriber(modid = Rcf.ID, value = [Dist.CLIENT])
object GameEvents {
	@SubscribeEvent
	fun onRegisterClientReloadListeners(event: RegisterClientReloadListenersEvent) {
		PlayerAnimationSetup.refresh()
	}
}