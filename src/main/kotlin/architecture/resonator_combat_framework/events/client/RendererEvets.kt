package architecture.resonator_combat_framework.events.client

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.render.RcfFirstPersonRender
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderArmEvent
import net.neoforged.neoforge.client.event.RenderHandEvent

@EventBusSubscriber(modid = RcfConstants.ID, value = [Dist.CLIENT])
object RendererEvets {
	@SubscribeEvent
	fun renderHand(event: RenderHandEvent) {
		if (RcfFirstPersonRender.isFirstPersonPass()) {
			event.isCanceled = true
		}
	}

	@SubscribeEvent
	fun renderArm(event: RenderArmEvent) {
		val minecraft = Minecraft.getInstance()
		if (RcfFirstPersonRender.isFirstPersonPass() && event.player == minecraft.player) {
			event.isCanceled = true
		}
	}
}
