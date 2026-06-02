package architecture.resonator_combat_framework.events.registry

import architecture.goldenboughs_lib.events.registry.PayloadRegistry.playToServerAndClient
import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.network.*
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

@EventBusSubscriber(modid = RcfConstants.ID)
object PayloadRegistry {
	@SubscribeEvent
	fun register(event: RegisterPayloadHandlersEvent) {
		val registrar = event.registrar("1.0")
		registrar.playToServerAndClient(StopPlayerPayload.TYPE, StopPlayerPayload.STREAM_CODEC)
		registrar.playToServerAndClient(PlayPlayerPayload.TYPE, PlayPlayerPayload.STREAM_CODEC)
		registrar.playToServerAndClient(TriggerPlayerPayload.TYPE, TriggerPlayerPayload.STREAM_CODEC)
		registrar.playToServerAndClient(PausePlayerPayload.TYPE, PausePlayerPayload.STREAM_CODEC)
		registrar.playToServerAndClient(ResumePlayerPayload.TYPE, ResumePlayerPayload.STREAM_CODEC)
		RcfConstants.LOGGER.info("Registering payloads finish")
	}
}
