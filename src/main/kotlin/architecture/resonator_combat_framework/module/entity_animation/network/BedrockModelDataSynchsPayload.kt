package architecture.resonator_combat_framework.module.entity_animation.network

import architecture.goldenboughs_lib.api.payload.ToClientPayload
import architecture.goldenboughs_lib.util.LibUtil.MAP_RESOURCE_LOCATION_COMPOUND_TAG_CODEC
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockModelRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import io.netty.buffer.ByteBuf
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

data class BedrockModelDataSynchsPayload(
	val nbtMap: Map<ResourceLocation, CompoundTag>
) : ToClientPayload {
	companion object {
		@JvmField
		val TYPE = CustomPacketPayload.Type<BedrockModelDataSynchsPayload>(RcfUtil.modRl("bedrock_model_data_synchs"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, BedrockModelDataSynchsPayload> = StreamCodec.composite(
			MAP_RESOURCE_LOCATION_COMPOUND_TAG_CODEC, BedrockModelDataSynchsPayload::nbtMap,
			::BedrockModelDataSynchsPayload
		)
	}

	constructor() : this(BedrockModelRegistry.getInstance(false).getNbtCache())

	override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?> = TYPE

	override fun work(
		context: IPayloadContext,
		player: AbstractClientPlayer
	) {
		BedrockModelRegistry.getInstance(true).clearNbtCache()
	}
}
