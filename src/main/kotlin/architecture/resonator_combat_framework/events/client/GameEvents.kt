package architecture.resonator_combat_framework.events.client

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigLoader
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent

@EventBusSubscriber(modid = Rcf.ID, value = [Dist.CLIENT])
object GameEvents {
	@SubscribeEvent
	fun onRegisterClientReloadListeners(event: RegisterClientReloadListenersEvent) {
		event.registerReloadListener(ProxyBoneConfigLoader.getInstance(true))
	}
}
