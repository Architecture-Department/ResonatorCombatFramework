package architecture.resonator_combat_framework.module.player_animation.payload

import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.player_animation.mixed.PlayerProxyProvider.Companion.getAnimationTransformer
import io.netty.buffer.ByteBuf
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

data class StopPlayerPayload(
	val playerUuid: UUID,
	val controllerName: Optional<String>,
	val fadeOutTicks: Int = -1
) : ToServerAndClientPayload {

	constructor(
		playerUuid: UUID,
		controllerName: String?,
		fadeOutTicks: Int = -1
	) : this(playerUuid, Optional.ofNullable(controllerName), fadeOutTicks)

	override fun type() = TYPE

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val level = context.player().level()
		val target = (level.getPlayerByUUID(playerUuid) as? AbstractClientPlayer) ?: return
		val transformer = target.getAnimationTransformer()
		if (controllerName.isPresent) {
			transformer.getController(controllerName.get()).stop(fadeOutTicks)
		} else {
			transformer.stopAll(fadeOutTicks)
		}
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		player.getAnimationTransformer().let { transformer ->
			if (controllerName.isPresent) {
				transformer.getController(controllerName.get()).stop(fadeOutTicks)
			} else {
				transformer.stopAll(fadeOutTicks)
			}
		}
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
			player, StopPlayerPayload(player.uuid, controllerName, fadeOutTicks)
		)
	}

	companion object {
		private val OPTIONAL_STRING: StreamCodec<ByteBuf, Optional<String>> =
			StreamCodec.of(
				{ buf, v ->
					buf.writeBoolean(v.isPresent)
					v.ifPresent { ByteBufCodecs.STRING_UTF8.encode(buf, it) }
				},
				{ buf ->
					if (buf.readBoolean()) Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf))
					else Optional.empty()
				}
			)

		@JvmField
		val TYPE = CustomPacketPayload.Type<StopPlayerPayload>(RcfConstants.modRl("stop_player"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, StopPlayerPayload> = StreamCodec.of(
			{ buf, p ->
				UUIDUtil.STREAM_CODEC.encode(buf, p.playerUuid)
				OPTIONAL_STRING.encode(buf, p.controllerName)
				ByteBufCodecs.VAR_INT.encode(buf, p.fadeOutTicks)
			},
			{ buf ->
				StopPlayerPayload(
					UUIDUtil.STREAM_CODEC.decode(buf),
					OPTIONAL_STRING.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf)
				)
			}
		)
	}
}