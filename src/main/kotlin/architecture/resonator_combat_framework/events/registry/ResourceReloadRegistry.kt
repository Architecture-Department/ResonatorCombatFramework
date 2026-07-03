package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.common.registry.ItemPropertyRegistry
import architecture.resonator_combat_framework.module.animation.AnimationDef
import architecture.resonator_combat_framework.module.animation.event.AnimationDefRegistryEvent
import architecture.resonator_combat_framework.module.animation.registry.AnimationDefRegistry
import architecture.resonator_combat_framework.module.animation.registry.BoneConfigRegistry
import architecture.resonator_combat_framework.module.animation.registry.GeometryModelRegistry
import architecture.resonator_combat_framework.module.animation.registry.KeyframeAnimationRegistry
import architecture.resonator_combat_framework.module.animation.util.GeckoLibCacheServer
import architecture.resonator_combat_framework.module.state_machine.event.ActionRegistryEvent
import architecture.resonator_combat_framework.module.state_machine.event.ActionSequenceRegistryEvent
import architecture.resonator_combat_framework.module.state_machine.registry.ActionRegistry
import architecture.resonator_combat_framework.module.state_machine.registry.ActionSequenceRegistry
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
		event.addListener(BoneConfigRegistry.getInstance(false))
		event.addListener(KeyframeAnimationRegistry.getInstance(false))
		event.addListener(GeometryModelRegistry.getInstance(false))
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	fun registryLowest(event: AddReloadListenerEvent) {
		event.addListener(AnimationDefRegistry)
		event.addListener(ActionRegistry)
		event.addListener(ActionSequenceRegistry)
		event.addListener(ItemPropertyRegistry)
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	fun registry(event: AnimationDefRegistryEvent) {
		KeyframeAnimationRegistry.findAll().forEach { (k, v) ->
			event.register(k, ::AnimationDef)
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	fun registry(event: ActionRegistryEvent) {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	fun registry(event: ActionSequenceRegistryEvent) {
	}
}



