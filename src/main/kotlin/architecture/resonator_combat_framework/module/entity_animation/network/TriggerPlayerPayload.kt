package architecture.resonator_combat_framework.module.entity_animation.network


import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.goldenboughs_lib.util.LibUtil.OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC
import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.mixed.IAnimationProxyProvider.Companion.getAnimationTransformer
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
	val animId: String,
	val speedMultiplier: Float = 1f,
	val fadeInTicks: Int = -1,
	val fadeOutTicks: Int = -1
) : ToServerAndClientPayload {

	@JvmOverloads
	constructor(
		playerUuid: UUID,
		controllerName: ResourceLocation?,
		animId: String,
		speedMultiplier: Float = 1f,
		fadeInTicks: Int = -1,
		fadeOutTicks: Int = -1
	) : this(playerUuid, Optional.ofNullable(controllerName), animId, speedMultiplier, fadeInTicks, fadeOutTicks)

	override fun type() = TYPE

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val level = context.player().level()
		val target = (level.getPlayerByUUID(playerUuid) as? AbstractClientPlayer) ?: return
		val config = AnimationPlayData(
			animId = animId,
			controllerName = controllerName.orElse(AnimationControllers.MAIN)!!,
			speedMultiplier = speedMultiplier,
			fadeInTicks = fadeInTicks,
			fadeOutTicks = fadeOutTicks
		)
		target.getAnimationTransformer().trigger(config)
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		val config = AnimationPlayData(
			animId = animId,
			controllerName = controllerName.orElse(AnimationControllers.MAIN)!!,
			speedMultiplier = speedMultiplier,
			fadeInTicks = fadeInTicks,
			fadeOutTicks = fadeOutTicks
		)
		player.getAnimationTransformer().trigger(config)
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, this)
	}

	companion object {
		@JvmField
		val TYPE = CustomPacketPayload.Type<TriggerPlayerPayload>(RcfConstants.modRl("trigger_player"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, TriggerPlayerPayload> = StreamCodec.of(
			{ buf, p ->
				UUIDUtil.STREAM_CODEC.encode(buf, p.playerUuid)
				OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC.encode(buf, p.controllerName)
				ByteBufCodecs.STRING_UTF8.encode(buf, p.animId)
				ByteBufCodecs.FLOAT.encode(buf, p.speedMultiplier)
				ByteBufCodecs.VAR_INT.encode(buf, p.fadeInTicks)
				ByteBufCodecs.VAR_INT.encode(buf, p.fadeOutTicks)
			},
			{ buf ->
				TriggerPlayerPayload(
					UUIDUtil.STREAM_CODEC.decode(buf),
					OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.FLOAT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf)
				)
			}
		)
	}
}

