package architecture.resonator_combat_framework.events.registry.client

import architecture.resonator_combat_framework.common.registry.ItemPropertyRegistry
import architecture.resonator_combat_framework.module.animation.registry.BoneConfigRegistry
import architecture.resonator_combat_framework.module.animation.registry.GeometryModelRegistry
import architecture.resonator_combat_framework.module.animation.registry.KeyframeAnimationRegistry
import architecture.resonator_combat_framework.module.state_machine.registry.ActionRegistry
import architecture.resonator_combat_framework.module.state_machine.registry.ActionSequenceRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent

/**
 * 客户端资源重载注册 —— 注册需要随资源包重载而重新加载的监听器。
 */
@EventBusSubscriber(modid = RcfUtil.ID, value = [Dist.CLIENT])
object ResourceReloadRegistry {
	@SubscribeEvent
	fun registry(event: RegisterClientReloadListenersEvent) {
		event.registerReloadListener(BoneConfigRegistry.getInstance(true))
		event.registerReloadListener(KeyframeAnimationRegistry.getInstance(true))
		event.registerReloadListener(GeometryModelRegistry.getInstance(true))
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	fun registryLowest(event: RegisterClientReloadListenersEvent) {
//		event.registerReloadListener(AnimationDefRegistry)
		event.registerReloadListener(ActionRegistry)
		event.registerReloadListener(ActionSequenceRegistry)
		event.registerReloadListener(ItemPropertyRegistry)
	}
}
