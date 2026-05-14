package architecture.resonator_combat_framework.module.player_animation.payload.toc

import architecture.goldenboughs_lib.api.payload.ToClientPayload
import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.helper.PlayerAnimationHelper
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

@JvmRecord
data class AnimatePlayerPayload(
	val id: String,
	val playerUUID: UUID
) : ToClientPayload {
	override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> {
		return TYPE
	}

	override fun work(context: IPayloadContext, player: Player) {
		val level = player.level()
		if (!level.isClientSide()) return
		val playerToAnimate = level.getPlayerByUUID(playerUUID) ?: return
		PlayerAnimationHelper.triggerPlayerAnimation(playerToAnimate, id)
	}

	companion object {
		@JvmField
		val TYPE: CustomPacketPayload.Type<AnimatePlayerPayload> = CustomPacketPayload.Type(
			ResourceLocation.fromNamespaceAndPath(Rcf.ID, "animate_player_packet")
		)

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, AnimatePlayerPayload> =
			StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8,
				AnimatePlayerPayload::id,
				ByteBufCodecs.STRING_UTF8.map(
					UUID::fromString,
					UUID::toString
				),
				AnimatePlayerPayload::playerUUID,
				::AnimatePlayerPayload
			)
	}
}