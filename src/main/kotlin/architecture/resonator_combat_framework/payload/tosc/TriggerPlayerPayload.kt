package architecture.resonator_combat_framework.payload.tosc


import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.goldenboughs_lib.util.LibUtil.RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC
import architecture.resonator_combat_framework.animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.animation.data.PlayConfig
import architecture.resonator_combat_framework.init.RcfAnimationControllers
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
 * 触发动画数据包（双向：服务端↔客户端）。
 * 触发指定玩家播放指定动画，支持速度倍率和淡入淡出控制。
 * 在服务端和所有追踪该玩家的客户端之间同步。
 *
 * @property playerUuid 目标玩家的 UUID
 * @property controllerName 目标控制器名称，为空则使用主控制器
 * @property animId 要触发的动画 ID
 * @property speedMultiplier 播放速度倍率
 * @property fadeInTime 淡入时长（秒），-1 表示默认
 * @property fadeOutTime 淡出时长（秒），-1 表示默认
 */
data class TriggerPlayerPayload
@JvmOverloads
constructor(
	val playerUuid: UUID,
	val controllerName: Optional<ResourceLocation>,
	val animId: ResourceLocation,
	val speedMultiplier: Float = 1f,
	val fadeInTime: Float = -1f,
	val fadeOutTime: Float = -1f
) : ToServerAndClientPayload {

	@JvmOverloads
	constructor(
		playerUuid: UUID,
		controllerName: ResourceLocation?,
		animId: ResourceLocation,
		speedMultiplier: Float = 1f,
		fadeInTime: Float = -1f,
		fadeOutTime: Float = -1f
	) : this(playerUuid, Optional.ofNullable(controllerName), animId, speedMultiplier, fadeInTime, fadeOutTime)

	override fun type() = TYPE

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val level = context.player().level()
		val target = (level.getPlayerByUUID(playerUuid) as? AbstractClientPlayer) ?: return
		val config = PlayConfig(
			speedMultiplier = speedMultiplier,
			fadeInTime = fadeInTime,
			fadeOutTime = fadeOutTime
		)
		target.getMapperProvider().trigger(controllerName.orElse(RcfAnimationControllers.MAIN)!!, animId, config)
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		val config = PlayConfig(
			speedMultiplier = speedMultiplier,
			fadeInTime = fadeInTime,
			fadeOutTime = fadeOutTime
		)
		player.getMapperProvider().trigger(controllerName.orElse(RcfAnimationControllers.MAIN)!!, animId, config)
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, this)
	}

	companion object {
		@JvmField
		val TYPE = CustomPacketPayload.Type<TriggerPlayerPayload>(RcfUtil.modRl("trigger_player"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, TriggerPlayerPayload> = StreamCodec.of(
			{ buf, p ->
				UUIDUtil.STREAM_CODEC.encode(buf, p.playerUuid)
				RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC.encode(buf, p.controllerName)
				ResourceLocation.STREAM_CODEC.encode(buf, p.animId)
				ByteBufCodecs.FLOAT.encode(buf, p.speedMultiplier)
				ByteBufCodecs.FLOAT.encode(buf, p.fadeInTime)
				ByteBufCodecs.FLOAT.encode(buf, p.fadeOutTime)
			},
			{ buf ->
				TriggerPlayerPayload(
					UUIDUtil.STREAM_CODEC.decode(buf),
					RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC.decode(buf),
					ResourceLocation.STREAM_CODEC.decode(buf),
					ByteBufCodecs.FLOAT.decode(buf),
					ByteBufCodecs.FLOAT.decode(buf),
					ByteBufCodecs.FLOAT.decode(buf)
				)
			}
		)
	}
}
