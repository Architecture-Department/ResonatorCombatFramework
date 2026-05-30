package architecture.resonator_combat_framework.module.player_animation.payload

// 播放动画数据包。从服务端发送到客户端，触发动画播放

import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.goldenboughs_lib.core.LibConstants.OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC
import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.events.registry.AnimationControllerRegistry
import architecture.resonator_combat_framework.module.player_animation.config.AnimType
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.mixed.PlayerProxyProvider.Companion.getAnimationTransformer
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

data class PlayPlayerPayload(
	val playerUuid: UUID,
	val controllerName: Optional<ResourceLocation>,
	val animId: String,
	val animType: AnimType = AnimType.DEFAULT,
	val speedMultiplier: Float = 1f,
	val startTime: Int = 0,
	val endTime: Int = 0,
	val fadeInTicks: Int = -1,
	val fadeOutTicks: Int = -1
) : ToServerAndClientPayload {

	constructor(
		playerUuid: UUID,
		controllerName: ResourceLocation?,
		animId: String,
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

	private fun buildConfig() = AnimationPlayConfig(
		animId = animId,
		controllerName = controllerName.orElse(AnimationControllerRegistry.DEFAULT)!!,
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
		target.getAnimationTransformer().trigger(buildConfig())
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		player.getAnimationTransformer().trigger(buildConfig())
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, this)
	}

	companion object {
		private val ANIM_TYPE_CODEC: StreamCodec<ByteBuf, AnimType> =
			ByteBufCodecs.BYTE.map(
				{ AnimType.entries[it.toInt()] },
				{ it.ordinal.toByte() }
			)

		@JvmField
		val TYPE = CustomPacketPayload.Type<PlayPlayerPayload>(RcfConstants.modRl("play_player"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, PlayPlayerPayload> = StreamCodec.of(
			{ buf, p ->
				UUIDUtil.STREAM_CODEC.encode(buf, p.playerUuid)
				OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC.encode(buf, p.controllerName)
				ByteBufCodecs.STRING_UTF8.encode(buf, p.animId)
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
					ByteBufCodecs.STRING_UTF8.decode(it),
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
