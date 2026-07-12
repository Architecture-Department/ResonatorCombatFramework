package architecture.resonator_combat_framework.payload.tosc


import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.goldenboughs_lib.util.LibUtil.RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC
import architecture.resonator_combat_framework.animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.init.registry.AnimationControllers
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

/**
 * 恢复动画数据包（双向：服务端↔客户端）。
 * 恢复指定玩家已暂停的动画控制器，并在服务端和所有追踪该玩家的客户端之间同步。
 *
 * @property playerUuid 目标玩家的 UUID
 * @property controllerName 要恢复的控制器名称，为空则恢复主控制器
 */
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
		target.getMapperProvider().resume(ctrlName)
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		player.getMapperProvider().resume(ctrlName)
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, this)
	}

	companion object {
		@JvmField
		val TYPE = CustomPacketPayload.Type<ResumePlayerPayload>(RcfUtil.modRl("resume_player"))

		@JvmField
		val STREAM_CODEC = StreamCodec.of<ByteBuf, ResumePlayerPayload>(
			{ buf, p ->
				UUIDUtil.STREAM_CODEC.encode(buf, p.playerUuid)
				RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC.encode(buf, p.controllerName)
			},
			{
				ResumePlayerPayload(
					UUIDUtil.STREAM_CODEC.decode(it),
					RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC.decode(it)
				)
			}
		)
	}
}
