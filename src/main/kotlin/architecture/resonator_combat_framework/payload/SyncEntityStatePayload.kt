package architecture.resonator_combat_framework.payload

import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * 实体状态同步数据包。
 * 服务端 -> 客户端，同步状态变更。
 *
 * @param entityId 实体 ID
 * @param layerId 层级标识符
 * @param stateId 状态标识符
 * @param timer 计时器值
 * @param immediate 是否立即同步
 */
data class SyncEntityStatePayload(
	val entityId: Int,
	val layerId: ResourceLocation,
	val stateId: ResourceLocation,
	val timer: Int,
	val immediate: Boolean
) : CustomPacketPayload {
	override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

	companion object {
		@JvmField
		val TYPE: CustomPacketPayload.Type<SyncEntityStatePayload> =
			CustomPacketPayload.Type(RcfUtil.modRl("sync_entity_state"))

		@JvmField
		val STREAM_CODEC: StreamCodec<in RegistryFriendlyByteBuf, SyncEntityStatePayload> =
			StreamCodec.composite(
				ByteBufCodecs.VAR_INT,
				SyncEntityStatePayload::entityId,
				ResourceLocation.STREAM_CODEC,
				SyncEntityStatePayload::layerId,
				ResourceLocation.STREAM_CODEC,
				SyncEntityStatePayload::stateId,
				ByteBufCodecs.VAR_INT,
				SyncEntityStatePayload::timer,
				ByteBufCodecs.BOOL,
				SyncEntityStatePayload::immediate,
				::SyncEntityStatePayload
			)
	}
}
