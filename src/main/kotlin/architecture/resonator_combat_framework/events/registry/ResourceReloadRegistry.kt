package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.common.registry.ItemPropertyRegistry
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.module.entity_animation.event.StaticAnimationRegistryEvent
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationDataRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockModelRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.StaticAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.util.GeckoLibCacheServer
import architecture.resonator_combat_framework.module.entity_state_machine.event.ActionRegistryEvent
import architecture.resonator_combat_framework.module.entity_state_machine.event.ActionSequenceRegistryEvent
import architecture.resonator_combat_framework.module.entity_state_machine.registry.ActionRegistry
import architecture.resonator_combat_framework.module.entity_state_machine.registry.ActionSequenceRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.AddReloadListenerEvent

@EventBusSubscriber(modid = RcfUtil.ID)
object ResourceReloadRegistry {
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	fun registry(event: AddReloadListenerEvent) {
		event.addListener(GeckoLibCacheServer::reload)
		event.addListener(BedrockAnimationDataRegistry.getInstance(false))
		event.addListener(BedrockAnimationRegistry.getInstance(false))
		event.addListener(BedrockModelRegistry.getInstance(false))
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	fun registryLowest(event: AddReloadListenerEvent) {
		event.addListener(StaticAnimationRegistry)
		event.addListener(ActionRegistry)
		event.addListener(ActionSequenceRegistry)
		event.addListener(ItemPropertyRegistry)
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	fun registry(event: StaticAnimationRegistryEvent) {
		BedrockAnimationRegistry.findAll().forEach { (k, v) ->
			event.register(k, ::StaticAnimation)
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	fun registry(event: ActionRegistryEvent) {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	fun registry(event: ActionSequenceRegistryEvent) {
	}
}



