package architecture.resonator_combat_framework.module.entity_animation.network


import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.goldenboughs_lib.util.LibUtil.OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC
import architecture.resonator_combat_framework.module.entity_animation.IProxyAnimationProvider.Companion.getAnimationTransformer
import architecture.resonator_combat_framework.util.RcfUtil
import io.netty.buffer.ByteBuf
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

/** 停止动画数据包。从服务端发送到客户端，停止指定动画*/
data class StopPlayerPayload
@JvmOverloads
constructor(
	val playerUuid: UUID,
	val controllerName: Optional<ResourceLocation> = Optional.empty(),
	val fadeOutTicks: Int = -1
) : ToServerAndClientPayload {

	@JvmOverloads
	constructor(
		playerUuid: UUID,
		controllerName: ResourceLocation?,
		fadeOutTicks: Int = -1
	) : this(playerUuid, Optional.ofNullable(controllerName), fadeOutTicks)

	override fun type() = TYPE

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val level = context.player().level()
		val target = (level.getPlayerByUUID(playerUuid) as? AbstractClientPlayer) ?: return
		val transformer = target.getAnimationTransformer()
		if (controllerName.isPresent) {
			transformer.getController(controllerName.get())?.stop(fadeOutTicks)
		} else {
			transformer.stopAll(fadeOutTicks)
		}
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		player.getAnimationTransformer().let { transformer ->
			if (controllerName.isPresent) {
				transformer.getController(controllerName.get())?.stop(fadeOutTicks)
			} else {
				transformer.stopAll(fadeOutTicks)
			}
		}
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
			player, StopPlayerPayload(player.uuid, controllerName, fadeOutTicks)
		)
	}

	companion object {

		@JvmField
		val TYPE = CustomPacketPayload.Type<StopPlayerPayload>(RcfUtil.modRl("stop_player"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, StopPlayerPayload> = StreamCodec.of(
			{ buf, p ->
				UUIDUtil.STREAM_CODEC.encode(buf, p.playerUuid)
				OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC.encode(buf, p.controllerName)
				ByteBufCodecs.VAR_INT.encode(buf, p.fadeOutTicks)
			},
			{ buf ->
				StopPlayerPayload(
					UUIDUtil.STREAM_CODEC.decode(buf),
					OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf)
				)
			}
		)
	}
}

