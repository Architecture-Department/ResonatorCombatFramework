package architecture.resonator_combat_framework.module.player_animation.payload

import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.clientStopPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.clientTriggerPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.serverStopPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.serverTriggerPlayerAnimation
import io.netty.buffer.ByteBuf
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

data class AnimatePlayerPayload(
	val id: String,
	val playerUuid: UUID,
	val speedMultiplier: Float = 1f,
	val useDuration: Boolean = false,
	val durationTicks: Int = 0,
	val originalAnimLengthSec: Float = 0f
) : ToServerAndClientPayload {

	override fun type() = TYPE

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val level: Level = context.player().level()
		val target = (level.getPlayerByUUID(playerUuid) as? AbstractClientPlayer) ?: return
		if (id == STOP_MARKER || id.isEmpty()) {
			target.clientStopPlayerAnimation()
		} else {
			target.clientTriggerPlayerAnimation(
				if (useDuration)
					AnimationPlayConfig.builder(id).duration(durationTicks, originalAnimLengthSec).build()
				else
					AnimationPlayConfig.of(id).copy(speedMultiplier = speedMultiplier)
			)
		}
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		if (id == STOP_MARKER || id.isEmpty()) {
			player.serverStopPlayerAnimation()
		} else {
			player.serverTriggerPlayerAnimation(
				if (useDuration)
					AnimationPlayConfig.builder(id).duration(durationTicks, originalAnimLengthSec).build()
				else
					AnimationPlayConfig.of(id).copy(speedMultiplier = speedMultiplier)
			)
		}
	}

	companion object {
		const val STOP_MARKER = ""

		@JvmStatic
		fun stop() = AnimatePlayerPayload(STOP_MARKER, UUID.randomUUID())

		@JvmField
		val TYPE = CustomPacketPayload.Type<AnimatePlayerPayload>(RcfConstants.modRl("animate_player"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, AnimatePlayerPayload> = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, AnimatePlayerPayload::id,
			ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), AnimatePlayerPayload::playerUuid,
			ByteBufCodecs.FLOAT, AnimatePlayerPayload::speedMultiplier,
			ByteBufCodecs.BOOL, AnimatePlayerPayload::useDuration,
			ByteBufCodecs.VAR_INT, AnimatePlayerPayload::durationTicks,
			ByteBufCodecs.FLOAT, AnimatePlayerPayload::originalAnimLengthSec,
			::AnimatePlayerPayload
		)
	}
}
