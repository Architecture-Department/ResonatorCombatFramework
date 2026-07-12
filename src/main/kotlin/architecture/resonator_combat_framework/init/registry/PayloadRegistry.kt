package architecture.resonator_combat_framework.init.registry

import architecture.goldenboughs_lib.events.registry.PayloadRegistry.playToClient
import architecture.goldenboughs_lib.events.registry.PayloadRegistry.playToServerAndClient
import architecture.resonator_combat_framework.payload.toc.BoneConfigSynchPayload
import architecture.resonator_combat_framework.payload.toc.GeometryModelSynchPayload
import architecture.resonator_combat_framework.payload.toc.KeyframeAnimationSynchPayload
import architecture.resonator_combat_framework.payload.tosc.*
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

/**
 * 网络数据包注册 —— 在 [RegisterPayloadHandlersEvent] 中注册所有自定义网络数据包。
 */
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
		registrar.playToClient(KeyframeAnimationSynchPayload.TYPE, KeyframeAnimationSynchPayload.STREAM_CODEC)
		registrar.playToClient(GeometryModelSynchPayload.TYPE, GeometryModelSynchPayload.STREAM_CODEC)
		registrar.playToClient(
			BoneConfigSynchPayload.TYPE,
			BoneConfigSynchPayload.STREAM_CODEC
		)
//		registrar.playToServerAndClient(SyncEntityStatePayload.TYPE, SyncEntityStatePayload.STREAM_CODEC)
		registrar.playToServerAndClient(AttackPayload.TYPE, AttackPayload.STREAM_CODEC)
		RcfUtil.LOGGER.info("Registering payloads finish")
	}
}
