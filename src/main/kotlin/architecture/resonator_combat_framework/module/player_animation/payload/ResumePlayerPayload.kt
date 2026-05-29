package architecture.resonator_combat_framework.module.player_animation.payload

// 恢复动画数据包

import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.player_animation.mixed.PlayerProxyProvider.Companion.getAnimationTransformer
import io.netty.buffer.ByteBuf
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.*

data class ResumePlayerPayload(
	val playerUuid: UUID,
	val controllerName: Optional<String>
) : ToServerAndClientPayload {

	constructor(playerUuid: UUID, controllerName: String?) : this(playerUuid, Optional.ofNullable(controllerName))

	override fun type() = TYPE

	private val ctrlName: String get() = controllerName.orElse("default")

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val target = context.player().level().getPlayerByUUID(playerUuid) as? AbstractClientPlayer ?: return
		target.getAnimationTransformer().resume(ctrlName)
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		player.getAnimationTransformer().resume(ctrlName)
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, this)
	}

	companion object {
		private val OPTIONAL_STRING = StreamCodec.of<ByteBuf, Optional<String>>(
			{ buf, v -> buf.writeBoolean(v.isPresent); v.ifPresent { ByteBufCodecs.STRING_UTF8.encode(buf, it) } },
			{ buf -> if (buf.readBoolean()) Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf)) else Optional.empty() }
		)

		@JvmField
		val TYPE = CustomPacketPayload.Type<ResumePlayerPayload>(RcfConstants.modRl("resume_player"))

		@JvmField
		val STREAM_CODEC = StreamCodec.of<ByteBuf, ResumePlayerPayload>(
			{ buf, p -> UUIDUtil.STREAM_CODEC.encode(buf, p.playerUuid); OPTIONAL_STRING.encode(buf, p.controllerName) },
			{ buf -> ResumePlayerPayload(UUIDUtil.STREAM_CODEC.decode(buf), OPTIONAL_STRING.decode(buf)) }
		)
	}
}
