package architecture.resonator_combat_framework.events.registry.client

import architecture.resonator_combat_framework.init.RcfRegistries
import architecture.resonator_combat_framework.init.RcfStaticAnimations
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockModelRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.ProxyBoneConfigDataRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent

@EventBusSubscriber(modid = RcfUtil.ID, value = [Dist.CLIENT])
object ResourceReloadRegistry {
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	fun registryHighest(event: RegisterClientReloadListenersEvent) {
		RcfRegistries.getStaticAnimations(true).clear()
	}

	@SubscribeEvent
	fun registry(event: RegisterClientReloadListenersEvent) {
		event.registerReloadListener(ProxyBoneConfigDataRegistry.getInstance(true))
		event.registerReloadListener(BedrockAnimationRegistry.getInstance(true))
		event.registerReloadListener(BedrockModelRegistry.getInstance(true))

		RcfStaticAnimations.init(true)
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	fun registryLowest(event: RegisterClientReloadListenersEvent) {
		BedrockAnimationRegistry.getInstance(true).getAllStaticAnim().forEach { (_, animation) ->
			RcfStaticAnimations.register(animation.id, { animation }, true)
		}
	}
}