package architecture.resonator_combat_framework.core.registry.client

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.player_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.module.player_animation.registry.BedrockModelRegistry
import architecture.resonator_combat_framework.module.player_animation.registry.ProxyBoneConfigRegistry
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent

@EventBusSubscriber(modid = RcfConstants.ID, value = [Dist.CLIENT])
object ResourceReloadRegistry {
	@SubscribeEvent
	fun registry(event: RegisterClientReloadListenersEvent) {
		event.registerReloadListener(ProxyBoneConfigRegistry.getInstance(true))
		event.registerReloadListener(BedrockAnimationRegistry.getInstance(true))
		event.registerReloadListener(BedrockModelRegistry.getInstance(true))
	}
}