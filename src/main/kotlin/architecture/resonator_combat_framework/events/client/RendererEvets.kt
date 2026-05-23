package architecture.resonator_combat_framework.events.client

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.firstPerson.RcfFirstPersonRender
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderHandEvent

@EventBusSubscriber(modid = Rcf.ID, value = [Dist.CLIENT])
object RendererEvets {
	@SubscribeEvent
	fun renderHand(event: RenderHandEvent) {
		if (RcfFirstPersonRender.isFirstPersonPass()) {
			event.isCanceled = true
		}
	}
}