package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.event.AnimationRegistry
import architecture.resonator_combat_framework.init.RcfStaticAnimations
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockModelRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.ProxyBoneConfigDataRegistry
import architecture.resonator_combat_framework.module.entity_animation.util.GeckoLibCacheServer
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.AddReloadListenerEvent

@EventBusSubscriber(modid = RcfUtil.ID)
object ResourceReloadRegistry {
	@SubscribeEvent
	fun registry(event: AddReloadListenerEvent) {
		event.addListener(GeckoLibCacheServer::reload)
		event.addListener(ProxyBoneConfigDataRegistry.getInstance(false))
		event.addListener(BedrockAnimationRegistry.getInstance(false))
		event.addListener(BedrockModelRegistry.getInstance(false))
	}

	@SubscribeEvent
	fun staticAnimationRegistry(event: AnimationRegistry) {
		event.registerReloadListener {
			RcfStaticAnimations.register()
		}
	}
}



