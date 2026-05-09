package architecture.resonator_combat_framework.core.registry;

import architecture.goldenboughs_lib.common.payload.api.ToClientPayload;
import architecture.goldenboughs_lib.common.payload.api.ToServerAndClientPayload;
import architecture.goldenboughs_lib.common.payload.api.ToServerPayload;
import architecture.resonator_combat_framework.core.Rcf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = Rcf.ID)
public final class PayloadRegistry {
	@SubscribeEvent
	public static void register(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1.0");
		// 接收来自服务端和客户端的数据 发送到 客户端和服务端

		// 接收来自服务端的数据 发送到 客户端

		// 接收来自客户端的数据 发送到 服务端
		Rcf.LOGGER.info("Registering payloads finish");
	}

	private static <T extends ToServerAndClientPayload> @NotNull PayloadRegistrar playToServerAndClient(PayloadRegistrar registrar, CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> reader) {
		return registrar.playBidirectional(type, reader,
			new DirectionalPayloadHandler<>(ToServerAndClientPayload::handle, ToServerAndClientPayload::handle));
	}

	private static <T extends ToServerPayload> @NotNull PayloadRegistrar playToServer(PayloadRegistrar registrar, CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> reader) {
		return registrar.playToServer(type, reader, ToServerAndClientPayload::handle);
	}

	private static <T extends ToClientPayload> @NotNull PayloadRegistrar playToClient(PayloadRegistrar registrar, CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> reader) {
		return registrar.playToClient(type, reader, ToServerAndClientPayload::handle);
	}
}
