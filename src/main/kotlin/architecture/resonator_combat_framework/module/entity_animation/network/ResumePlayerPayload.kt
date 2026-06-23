package architecture.resonator_combat_framework.module.entity_animation.network


import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.goldenboughs_lib.util.LibUtil.OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.mixed.IAnimationProxyProvider.Companion.getAnimationTransformer
import architecture.resonator_combat_framework.util.RcfUtil
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

/** 恢复动画数据包 */
data class ResumePlayerPayload
@JvmOverloads
constructor(
	val playerUuid: UUID,
	val controllerName: Optional<ResourceLocation> = Optional.empty(),
) : ToServerAndClientPayload {

	constructor(playerUuid: UUID, controllerName: ResourceLocation?) : this(
		playerUuid,
		Optional.ofNullable(controllerName)
	)

	override fun type() = TYPE

	private val ctrlName: ResourceLocation get() = controllerName.orElse(AnimationControllers.MAIN)!!

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val target = context.player().level().getPlayerByUUID(playerUuid) as? AbstractClientPlayer ?: return
		target.getAnimationTransformer().resume(ctrlName)
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		player.getAnimationTransformer().resume(ctrlName)
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, this)
	}

	companion object {
		@JvmField
		val TYPE = CustomPacketPayload.Type<ResumePlayerPayload>(RcfUtil.modRl("resume_player"))

		@JvmField
		val STREAM_CODEC = StreamCodec.of<ByteBuf, ResumePlayerPayload>(
			{ buf, p ->
				UUIDUtil.STREAM_CODEC.encode(buf, p.playerUuid)
				OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC.encode(buf, p.controllerName)
			},
			{
				ResumePlayerPayload(
					UUIDUtil.STREAM_CODEC.decode(it),
					OPTIONAL_RESOURCE_LOCATION_STREAM_CODEC.decode(it)
				)
			}
		)
	}
}

