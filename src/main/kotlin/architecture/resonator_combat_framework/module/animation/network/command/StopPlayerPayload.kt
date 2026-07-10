package architecture.resonator_combat_framework.module.animation.network.command


import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.goldenboughs_lib.util.LibUtil.RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC
import architecture.resonator_combat_framework.module.animation.IAnimationProvider.Companion.getMapperProvider
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

/**
 * 停止动画数据包（双向：服务端↔客户端）。
 * 停止指定玩家的动画播放，可指定停止单个控制器或所有控制器，
 * 并支持淡出时长。在服务端和所有追踪该玩家的客户端之间同步。
 *
 * @property playerUuid 目标玩家的 UUID
 * @property controllerName 要停止的控制器名称，为空则停止所有控制器
 * @property fadeOutTime 淡出时长（秒），-1 表示立即停止
 */
data class StopPlayerPayload
@JvmOverloads
constructor(
	val playerUuid: UUID,
	val controllerName: Optional<ResourceLocation> = Optional.empty(),
	val fadeOutTime: Float = -1f
) : ToServerAndClientPayload {

	@JvmOverloads
	constructor(
		playerUuid: UUID,
		controllerName: ResourceLocation?,
		fadeOutTime: Float = -1f
	) : this(playerUuid, Optional.ofNullable(controllerName), fadeOutTime)

	override fun type() = TYPE

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val level = context.player().level()
		val target = (level.getPlayerByUUID(playerUuid) as? AbstractClientPlayer) ?: return
		val transformer = target.getMapperProvider()
		if (controllerName.isPresent) {
			transformer.getController(controllerName.get())?.stop(fadeOutTime)
		} else {
			transformer.stopAll(fadeOutTime)
		}
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		player.getMapperProvider().let { transformer ->
			if (controllerName.isPresent) {
				transformer.getController(controllerName.get())?.stop(fadeOutTime)
			} else {
				transformer.stopAll(fadeOutTime)
			}
		}
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
			player, StopPlayerPayload(player.uuid, controllerName, fadeOutTime)
		)
	}

	companion object {

		@JvmField
		val TYPE = CustomPacketPayload.Type<StopPlayerPayload>(RcfUtil.modRl("stop_player"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, StopPlayerPayload> = StreamCodec.of(
			{ buf, p ->
				UUIDUtil.STREAM_CODEC.encode(buf, p.playerUuid)
				RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC.encode(buf, p.controllerName)
				ByteBufCodecs.FLOAT.encode(buf, p.fadeOutTime)
			},
			{ buf ->
				StopPlayerPayload(
					UUIDUtil.STREAM_CODEC.decode(buf),
					RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC.decode(buf),
					ByteBufCodecs.FLOAT.decode(buf)
				)
			}
		)
	}
}
