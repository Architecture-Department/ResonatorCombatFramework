package architecture.resonator_combat_framework.module.entity_animation.network

// 暂停动画数据包

// 暂停动画数据包

import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.goldenboughs_lib.core.LibConstants.OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC
import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.events.registry.AnimationControllerRegistry
import architecture.resonator_combat_framework.module.entity_animation.mixed.PlayerProxyProvider.Companion.getAnimationTransformer
import io.netty.buffer.ByteBuf
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

data class PausePlayerPayload(
	val playerUuid: UUID,
	val controllerName: Optional<ResourceLocation>
) : ToServerAndClientPayload {

	constructor(playerUuid: UUID, controllerName: ResourceLocation?) : this(
		playerUuid,
		Optional.ofNullable(controllerName)
	)

	override fun type() = TYPE

	private val ctrlName: ResourceLocation get() = controllerName.orElse(AnimationControllerRegistry.MAIN)!!

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val target = context.player().level().getPlayerByUUID(playerUuid) as? AbstractClientPlayer ?: return
		target.getAnimationTransformer().pause(ctrlName)
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		player.getAnimationTransformer().pause(ctrlName)
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, this)
	}

	companion object {
		@JvmField
		val TYPE = CustomPacketPayload.Type<PausePlayerPayload>(RcfConstants.modRl("pause_player"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, PausePlayerPayload> = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, PausePlayerPayload::playerUuid,
			OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC, PausePlayerPayload::controllerName,
			::PausePlayerPayload
		)
	}
}

