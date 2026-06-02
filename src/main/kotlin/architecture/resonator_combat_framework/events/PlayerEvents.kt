package architecture.resonator_combat_framework.events

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.mixed.PlayerProxyProvider.Companion.getAnimationTransformer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.PlayerTickEvent

@EventBusSubscriber(modid = RcfConstants.ID)
object PlayerEvents {
	@SubscribeEvent
	fun tickPre(event: PlayerTickEvent.Post) {
		val player = event.entity
		player.getAnimationTransformer().tickAnimations()
	}
}
