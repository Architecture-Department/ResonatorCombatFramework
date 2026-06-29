package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.init.animation.RcfActionSequences
import architecture.resonator_combat_framework.init.animation.RcfActions
import architecture.resonator_combat_framework.init.animation.RcfStaticAnimations
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
	@SubscribeEvent
	fun registry(event: AddReloadListenerEvent) {
		event.addListener(GeckoLibCacheServer::reload)
		event.addListener(BedrockAnimationDataRegistry.getInstance(false))
		event.addListener(BedrockAnimationRegistry.getInstance(false))
		event.addListener(BedrockModelRegistry.getInstance(false))
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	fun registryLowest(event: AddReloadListenerEvent) {
		event.addListener(StaticAnimationRegistry.getInstance())
		event.addListener(ActionRegistry.getInstance())
		event.addListener(ActionSequenceRegistry.getInstance())
	}

	@SubscribeEvent
	fun registry(event: StaticAnimationRegistryEvent) {
		BedrockAnimationRegistry.getAll().forEach {
			event.register(RcfUtil.modRl(it.key), ::StaticAnimation)
		}
		RcfStaticAnimations.register(event)
	}

	@SubscribeEvent
	fun registry(event: ActionRegistryEvent) {
		RcfActions.register(event)
	}

	@SubscribeEvent
	fun registry(event: ActionSequenceRegistryEvent) {
		RcfActionSequences.register(event)
	}
}



