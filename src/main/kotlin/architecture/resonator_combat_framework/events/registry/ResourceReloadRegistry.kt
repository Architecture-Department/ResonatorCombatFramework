package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.GeckoLibCacheServer
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockModelRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.ProxyBoneConfigRegistry
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.AddReloadListenerEvent

@EventBusSubscriber(modid = RcfConstants.ID)
object ResourceReloadRegistry {
	@SubscribeEvent
	fun registry(event: AddReloadListenerEvent) {
		event.addListener(ProxyBoneConfigRegistry.getInstance(false))
		event.addListener(BedrockAnimationRegistry.getInstance(false))
		event.addListener(BedrockModelRegistry.getInstance(false))
		event.addListener(GeckoLibCacheServer::reload)
	}
}



