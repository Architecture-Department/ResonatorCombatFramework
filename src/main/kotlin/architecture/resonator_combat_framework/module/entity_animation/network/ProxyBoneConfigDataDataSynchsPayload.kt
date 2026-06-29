package architecture.resonator_combat_framework.module.entity_animation.network

import architecture.goldenboughs_lib.api.payload.ToClientPayload
import architecture.goldenboughs_lib.util.LibUtil.MAP_RESOURCE_LOCATION_COMPOUND_TAG_CODEC
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationDataRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import io.netty.buffer.ByteBuf
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

data class ProxyBoneConfigDataDataSynchsPayload(
	val nbtMap: Map<ResourceLocation, CompoundTag>
) : ToClientPayload {
	companion object {
		@JvmField
		val TYPE =
			CustomPacketPayload.Type<ProxyBoneConfigDataDataSynchsPayload>(RcfUtil.modRl("proxy_bone_config_data_data_synchs"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, ProxyBoneConfigDataDataSynchsPayload> = StreamCodec.composite(
			MAP_RESOURCE_LOCATION_COMPOUND_TAG_CODEC, ProxyBoneConfigDataDataSynchsPayload::nbtMap,
			::ProxyBoneConfigDataDataSynchsPayload
		)
	}

	constructor() : this(BedrockAnimationDataRegistry.getInstance(false).getNbtCache())

	override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?> = TYPE

	override fun work(
		context: IPayloadContext,
		player: AbstractClientPlayer
	) {
		BedrockAnimationDataRegistry.getInstance(true).clearNbtCache()
	}
}
