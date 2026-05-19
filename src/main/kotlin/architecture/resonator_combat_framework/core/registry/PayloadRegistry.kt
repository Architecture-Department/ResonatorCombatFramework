package architecture.resonator_combat_framework.core.registry

import architecture.goldenboughs_lib.core.registry.PayloadRegistry.playToServerAndClient
import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.payload.AnimatePlayerPayload
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

@EventBusSubscriber(modid = Rcf.ID)
object PayloadRegistry {
	@SubscribeEvent
	fun register(event: RegisterPayloadHandlersEvent) {
		val registrar = event.registrar("1.0")
		registrar.playToServerAndClient(AnimatePlayerPayload.TYPE, AnimatePlayerPayload.STREAM_CODEC)
		Rcf.LOGGER.info("Registering payloads finish")
	}
}
