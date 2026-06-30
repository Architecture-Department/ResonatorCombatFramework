package architecture.resonator_combat_framework.module.entity_animation.network

import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.goldenboughs_lib.util.LibUtil.OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.IProxyAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimType
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimationPlayData
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

/**播放动画数据包。从服务端发送到客户端，触发动画播放*/
data class PlayPlayerPayload
@JvmOverloads
constructor(
	val playerUuid: UUID,
	val controllerName: Optional<ResourceLocation> = Optional.empty(),
	val animId: ResourceLocation,
	val animType: AnimType = AnimType.DEFAULT,
	val speedMultiplier: Float = 1f,
	val startTime: Int = 0,
	val endTime: Int = 0,
	val fadeInTicks: Int = -1,
	val fadeOutTicks: Int = -1
) : ToServerAndClientPayload {

	@JvmOverloads
	constructor(
		playerUuid: UUID,
		controllerName: ResourceLocation?,
		animId: ResourceLocation,
		animType: AnimType = AnimType.DEFAULT,
		speedMultiplier: Float = 1f,
		startTime: Int = 0,
		endTime: Int = 0,
		fadeInTicks: Int = -1,
		fadeOutTicks: Int = -1
	) : this(
		playerUuid,
		Optional.ofNullable(controllerName),
		animId,
		animType,
		speedMultiplier,
		startTime,
		endTime,
		fadeInTicks,
		fadeOutTicks
	)

	override fun type() = TYPE

	private fun buildConfig() = AnimationPlayData(
		animType = animType,
		speedMultiplier = speedMultiplier,
		startTime = startTime,
		endTime = endTime,
		fadeInTicks = fadeInTicks,
		fadeOutTicks = fadeOutTicks
	)

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val level = context.player().level()
		val target = (level.getPlayerByUUID(playerUuid) as? AbstractClientPlayer) ?: return
		target.getMapperProvider().trigger(controllerName.orElse(AnimationControllers.MAIN)!!, animId, buildConfig())
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		player.getMapperProvider().trigger(controllerName.orElse(AnimationControllers.MAIN)!!, animId, buildConfig())
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, this)
	}

	companion object {
		private val ANIM_TYPE_CODEC: StreamCodec<ByteBuf, AnimType> =
			ByteBufCodecs.BYTE.map(
				{ AnimType.entries[it.toInt()] },
				{ it.ordinal.toByte() }
			)

		@JvmField
		val TYPE = CustomPacketPayload.Type<PlayPlayerPayload>(RcfUtil.modRl("play_player"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, PlayPlayerPayload> = StreamCodec.of(
			{ buf, p ->
				UUIDUtil.STREAM_CODEC.encode(buf, p.playerUuid)
				OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC.encode(buf, p.controllerName)
				ResourceLocation.STREAM_CODEC.encode(buf, p.animId)
				ANIM_TYPE_CODEC.encode(buf, p.animType)
				ByteBufCodecs.FLOAT.encode(buf, p.speedMultiplier)
				ByteBufCodecs.VAR_INT.encode(buf, p.startTime)
				ByteBufCodecs.VAR_INT.encode(buf, p.endTime)
				ByteBufCodecs.VAR_INT.encode(buf, p.fadeInTicks)
				ByteBufCodecs.VAR_INT.encode(buf, p.fadeOutTicks)
			},
			{
				PlayPlayerPayload(
					UUIDUtil.STREAM_CODEC.decode(it),
					OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC.decode(it),
					ResourceLocation.STREAM_CODEC.decode(it),
					ANIM_TYPE_CODEC.decode(it),
					ByteBufCodecs.FLOAT.decode(it),
					ByteBufCodecs.VAR_INT.decode(it),
					ByteBufCodecs.VAR_INT.decode(it),
					ByteBufCodecs.VAR_INT.decode(it),
					ByteBufCodecs.VAR_INT.decode(it)
				)
			}
		)
	}
}

