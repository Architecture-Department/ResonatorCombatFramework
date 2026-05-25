package architecture.resonator_combat_framework.events

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.GeckoLibCacheServer
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigLoader
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.AddReloadListenerEvent
import architecture.resonator_combat_framework.core.RcfConstants

@EventBusSubscriber(modid = RcfConstants.ID)
object GameEvents {
	@SubscribeEvent
	fun onAddReloadListener(event: AddReloadListenerEvent) {
		event.addListener(ProxyBoneConfigLoader.getInstance(false))
		event.addListener(GeckoLibCacheServer::reload)
	}
}