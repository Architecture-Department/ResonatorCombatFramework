package architecture.resonator_combat_framework.module.entity_animation.network

import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.goldenboughs_lib.util.LibUtil.RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayConfig
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayMode
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

/**
 * 播放动画数据包（双向：服务端↔客户端）。
 * 从服务端发送到客户端，触发指定玩家的动画播放。包含完整的播放配置：
 * 播放模式、速度倍率、起止时间和淡入淡出。
 *
 * @property playerUuid 目标玩家的 UUID
 * @property controllerName 目标控制器名称，为空则使用主控制器
 * @property animId 要播放的动画 ID
 * @property playMode 播放模式
 * @property speedMultiplier 播放速度倍率
 * @property startTime 起始时间（tick）
 * @property endTime 结束时间（tick）
 * @property fadeInTicks 淡入时长（tick），-1 表示默认
 * @property fadeOutTicks 淡出时长（tick），-1 表示默认
 */
data class PlayPlayerPayload
@JvmOverloads
constructor(
	val playerUuid: UUID,
	val controllerName: Optional<ResourceLocation> = Optional.empty(),
	val animId: ResourceLocation,
	val playMode: PlayMode = PlayMode.DEFAULT,
	val speedMultiplier: Float = 1f,
	val startTime: Int = 0,
	val endTime: Int = 0,
	val fadeInTicks: Int = -1,
	val fadeOutTicks: Int = -1
) : ToServerAndClientPayload {

	@JvmOverloads
	constructor(
		playerUuid: UUID,
		controllerName: ResourceLocation?,
		animId: ResourceLocation,
		playMode: PlayMode = PlayMode.DEFAULT,
		speedMultiplier: Float = 1f,
		startTime: Int = 0,
		endTime: Int = 0,
		fadeInTicks: Int = -1,
		fadeOutTicks: Int = -1
	) : this(
		playerUuid,
		Optional.ofNullable(controllerName),
		animId,
		playMode,
		speedMultiplier,
		startTime,
		endTime,
		fadeInTicks,
		fadeOutTicks
	)

	override fun type() = TYPE

	private fun buildConfig() = PlayConfig(
		playMode = playMode,
		speedMultiplier = speedMultiplier,
		startTime = startTime,
		endTime = endTime,
		fadeInTicks = fadeInTicks,
		fadeOutTicks = fadeOutTicks
	)

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		val level = context.player().level()
		val target = (level.getPlayerByUUID(playerUuid) as? AbstractClientPlayer) ?: return
		target.getMapperProvider().trigger(controllerName.orElse(AnimationControllers.MAIN)!!, animId, buildConfig())
	}

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		player.getMapperProvider().trigger(controllerName.orElse(AnimationControllers.MAIN)!!, animId, buildConfig())
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, this)
	}

	companion object {
		private val ANIM_TYPE_CODEC: StreamCodec<ByteBuf, PlayMode> =
			ByteBufCodecs.BYTE.map(
				{ PlayMode.entries[it.toInt()] },
				{ it.ordinal.toByte() }
			)

		@JvmField
		val TYPE = CustomPacketPayload.Type<PlayPlayerPayload>(RcfUtil.modRl("play_player"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, PlayPlayerPayload> = StreamCodec.of(
			{ buf, p ->
				UUIDUtil.STREAM_CODEC.encode(buf, p.playerUuid)
				RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC.encode(buf, p.controllerName)
				ResourceLocation.STREAM_CODEC.encode(buf, p.animId)
				ANIM_TYPE_CODEC.encode(buf, p.playMode)
				ByteBufCodecs.FLOAT.encode(buf, p.speedMultiplier)
				ByteBufCodecs.VAR_INT.encode(buf, p.startTime)
				ByteBufCodecs.VAR_INT.encode(buf, p.endTime)
				ByteBufCodecs.VAR_INT.encode(buf, p.fadeInTicks)
				ByteBufCodecs.VAR_INT.encode(buf, p.fadeOutTicks)
			},
			{
				PlayPlayerPayload(
					UUIDUtil.STREAM_CODEC.decode(it),
					RESOURCE_LOCATION_OPTIONAL_STREAM_CODEC.decode(it),
					ResourceLocation.STREAM_CODEC.decode(it),
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
