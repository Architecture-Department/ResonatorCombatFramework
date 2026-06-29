package architecture.resonator_combat_framework.events.registry

import architecture.goldenboughs_lib.events.registry.PayloadRegistry.playToClient
import architecture.goldenboughs_lib.events.registry.PayloadRegistry.playToServerAndClient
import architecture.resonator_combat_framework.module.entity_animation.network.*
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

@EventBusSubscriber(modid = RcfUtil.ID)
object PayloadRegistry {
	@SubscribeEvent
	fun register(event: RegisterPayloadHandlersEvent) {
		val registrar = event.registrar("1.0")
		registrar.playToServerAndClient(StopPlayerPayload.TYPE, StopPlayerPayload.STREAM_CODEC)
		registrar.playToServerAndClient(PlayPlayerPayload.TYPE, PlayPlayerPayload.STREAM_CODEC)
		registrar.playToServerAndClient(TriggerPlayerPayload.TYPE, TriggerPlayerPayload.STREAM_CODEC)
		registrar.playToServerAndClient(PausePlayerPayload.TYPE, PausePlayerPayload.STREAM_CODEC)
		registrar.playToServerAndClient(ResumePlayerPayload.TYPE, ResumePlayerPayload.STREAM_CODEC)
		registrar.playToClient(BedrockAnimationDataSynchsPayload.TYPE, BedrockAnimationDataSynchsPayload.STREAM_CODEC)
		registrar.playToClient(BedrockModelDataSynchsPayload.TYPE, BedrockModelDataSynchsPayload.STREAM_CODEC)
		registrar.playToClient(ProxyBoneConfigDataDataSynchsPayload.TYPE, ProxyBoneConfigDataDataSynchsPayload.STREAM_CODEC)
//		registrar.playToServerAndClient(SyncEntityStatePayload.TYPE, SyncEntityStatePayload.STREAM_CODEC)
		RcfUtil.LOGGER.info("Registering payloads finish")
	}
}
