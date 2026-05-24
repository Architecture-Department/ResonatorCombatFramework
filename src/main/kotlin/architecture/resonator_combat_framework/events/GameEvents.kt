package architecture.resonator_combat_framework.events

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.GeckoLibCacheServer
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigLoader
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.AddReloadListenerEvent

@EventBusSubscriber(modid = Rcf.ID)
object GameEvents {
	@SubscribeEvent
	fun onAddReloadListener(event: AddReloadListenerEvent) {
		event.addListener(ProxyBoneConfigLoader.getInstance(false))
		event.addListener(GeckoLibCacheServer::reload)
	}
}