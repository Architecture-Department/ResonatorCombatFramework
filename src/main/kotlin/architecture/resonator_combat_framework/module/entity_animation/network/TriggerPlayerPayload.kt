package architecture.resonator_combat_framework.module.entity_animation.network


import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.goldenboughs_lib.util.LibUtil.RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.IProxyAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayConfig
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

/** 触发动画数据包*/
data class TriggerPlayerPayload
@JvmOverloads
constructor(
	val playerUuid: UUID,
	val controllerName: Optional<ResourceLocation>,
	val animId: ResourceLocation,
	val speedMultiplier: Float = 1f,
	val fadeInTicks: Int = -1,
	val fadeOutTicks: Int = -1
) : ToServerAndClientPayload {

	@JvmOverloads
	constructor(
		playerUuid: UUID,
		controllerName: ResourceLocation?,
		animId: ResourceLocation,
		speedMultiplier: Float = 1f,
		fadeInTicks: Int = -1,
		fadeOutTicks: Int = -1
	) : this(playerUuid, Optional.ofNullable(controllerName), animId, speedMultiplier, fadeInTicks, fadeOutTicks)

	override fun type() = TYPE

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val level = context.player().level()
		val target = (level.getPlayerByUUID(playerUuid) as? AbstractClientPlayer) ?: return
		val config = PlayConfig(
			speedMultiplier = speedMultiplier,
			fadeInTicks = fadeInTicks,
			fadeOutTicks = fadeOutTicks
		)
		target.getMapperProvider().trigger(controllerName.orElse(AnimationControllers.MAIN)!!, animId, config)
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		val config = PlayConfig(
			speedMultiplier = speedMultiplier,
			fadeInTicks = fadeInTicks,
			fadeOutTicks = fadeOutTicks
		)
		player.getMapperProvider().trigger(controllerName.orElse(AnimationControllers.MAIN)!!, animId, config)
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
				ByteBufCodecs.VAR_INT.encode(buf, p.fadeInTicks)
				ByteBufCodecs.VAR_INT.encode(buf, p.fadeOutTicks)
			},
			{ buf ->
				TriggerPlayerPayload(
					UUIDUtil.STREAM_CODEC.decode(buf),
					RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC.decode(buf),
					ResourceLocation.STREAM_CODEC.decode(buf),
					ByteBufCodecs.FLOAT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf)
				)
			}
		)
	}
}

