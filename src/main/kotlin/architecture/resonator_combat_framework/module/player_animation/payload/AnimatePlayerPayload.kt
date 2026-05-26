package architecture.resonator_combat_framework.module.player_animation.payload

import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.clientStopPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.clientTriggerPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.pausePlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.resumePlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.serverStopPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.serverTriggerPlayerAnimation
import io.netty.buffer.ByteBuf
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

enum class AnimAction { PLAY, STOP, PAUSE, RESUME }

data class AnimatePlayerPayload(
	val action: AnimAction,
	val animId: String = "",
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
		when (action) {
			AnimAction.PLAY -> target.clientTriggerPlayerAnimation(
				if (useDuration) AnimationPlayConfig.builder(animId).duration(durationTicks, originalAnimLengthSec).build()
				else AnimationPlayConfig.of(animId).copy(speedMultiplier = speedMultiplier)
			)

			AnimAction.STOP -> target.clientStopPlayerAnimation()
			AnimAction.PAUSE -> target.pausePlayerAnimation()
			AnimAction.RESUME -> target.resumePlayerAnimation()
		}
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		when (action) {
			AnimAction.PLAY -> player.serverTriggerPlayerAnimation(
				if (useDuration) AnimationPlayConfig.builder(animId).duration(durationTicks, originalAnimLengthSec).build()
				else AnimationPlayConfig.of(animId).copy(speedMultiplier = speedMultiplier)
			)

			AnimAction.STOP -> player.serverStopPlayerAnimation()
			AnimAction.PAUSE -> player.pausePlayerAnimation()
			AnimAction.RESUME -> player.resumePlayerAnimation()
		}
	}

	companion object {
		private val ACTION_CODEC: StreamCodec<ByteBuf, AnimAction> = ByteBufCodecs.BYTE.map(
			{ AnimAction.entries[it.toInt()] },
			{ it.ordinal.toByte() }
		)

		@JvmStatic
		fun play(animId: String, playerUuid: UUID, speed: Float = 1f) =
			AnimatePlayerPayload(AnimAction.PLAY, animId, playerUuid, speed)

		@JvmStatic
		fun stop(playerUuid: UUID) = AnimatePlayerPayload(AnimAction.STOP, playerUuid = playerUuid)

		@JvmStatic
		fun pause(playerUuid: UUID) = AnimatePlayerPayload(AnimAction.PAUSE, playerUuid = playerUuid)

		@JvmStatic
		fun resume(playerUuid: UUID) = AnimatePlayerPayload(AnimAction.RESUME, playerUuid = playerUuid)

		@JvmField
		val TYPE = CustomPacketPayload.Type<AnimatePlayerPayload>(RcfConstants.modRl("animate_player"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, AnimatePlayerPayload> = NeoForgeStreamCodecs.composite(
			ACTION_CODEC, AnimatePlayerPayload::action,
			ByteBufCodecs.STRING_UTF8, AnimatePlayerPayload::animId,
			UUIDUtil.STREAM_CODEC, AnimatePlayerPayload::playerUuid,
			ByteBufCodecs.FLOAT, AnimatePlayerPayload::speedMultiplier,
			ByteBufCodecs.BOOL, AnimatePlayerPayload::useDuration,
			ByteBufCodecs.VAR_INT, AnimatePlayerPayload::durationTicks,
			ByteBufCodecs.FLOAT, AnimatePlayerPayload::originalAnimLengthSec,
			::AnimatePlayerPayload
		)
	}
}
