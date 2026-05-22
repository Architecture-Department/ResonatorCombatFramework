package architecture.resonator_combat_framework.module.player_animation.payload

import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.clientStopPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.clientTriggerPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.serverPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.serverStopPlayerAnimation
import io.netty.buffer.ByteBuf
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

// 双端网络包：服务端→客户端 / 客户端→服务端
data class AnimatePlayerPayload(val id: String, val playerUuid: UUID) : ToServerAndClientPayload {
	override fun type() = TYPE

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val level: Level = context.player().level()
		val target = (level.getPlayerByUUID(playerUuid) as? AbstractClientPlayer) ?: return
		if (id == STOP_MARKER || id.isEmpty()) target.clientStopPlayerAnimation()
		else target.clientTriggerPlayerAnimation(id)
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		if (id == STOP_MARKER || id.isEmpty()) player.serverStopPlayerAnimation()
		else player.serverPlayerAnimation(id)
	}

	companion object {
		const val STOP_MARKER = ""

		@JvmField
		val TYPE = CustomPacketPayload.Type<AnimatePlayerPayload>(
			ResourceLocation.fromNamespaceAndPath(Rcf.ID, "animate_player")
		)

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, AnimatePlayerPayload> = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, AnimatePlayerPayload::id,
			ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), AnimatePlayerPayload::playerUuid,
			::AnimatePlayerPayload
		)
	}
}
