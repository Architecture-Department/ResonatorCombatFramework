package architecture.resonator_combat_framework.events.registry.client

import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockModelRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.ProxyBoneConfigDataRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent

@EventBusSubscriber(modid = RcfUtil.ID, value = [Dist.CLIENT])
object ResourceReloadRegistry {
	@SubscribeEvent
	fun registry(event: RegisterClientReloadListenersEvent) {
		event.registerReloadListener(ProxyBoneConfigDataRegistry.getInstance(true))
		event.registerReloadListener(BedrockAnimationRegistry.getInstance(true))
		event.registerReloadListener(BedrockModelRegistry.getInstance(true))
	}
}