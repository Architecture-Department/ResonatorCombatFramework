package architecture.resonator_combat_framework.core.registry

import architecture.goldenboughs_lib.core.registry.PayloadRegistry.playToClient
import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.payload.toc.AnimatePlayerPayload
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

@EventBusSubscriber(modid = Rcf.ID)
object PayloadRegistry {
	@SubscribeEvent
	fun register(event: RegisterPayloadHandlersEvent) {
		val registrar = event.registrar("1.0")

		// 接收来自服务端和客户端的数据 发送到 客户端和服务端

		// 接收来自服务端的数据 发送到 客户端
		registrar.playToClient(
			AnimatePlayerPayload.TYPE,
			AnimatePlayerPayload.STREAM_CODEC
		)

		// 接收来自客户端的数据 发送到 服务端
		Rcf.LOGGER.info("Registering payloads finish")
	}
}
