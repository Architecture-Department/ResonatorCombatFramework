package architecture.resonator_combat_framework.module.entity_animation.network

import architecture.goldenboughs_lib.api.payload.ToClientPayload
import architecture.goldenboughs_lib.util.LibUtil.RESOURCE_LOCATION_BY_COMPOUND_TAG_MAP_STREAM_CODEC
import architecture.resonator_combat_framework.module.entity_animation.registry.GeometryModelRegistry
import architecture.resonator_combat_framework.util.RcfUtil
import com.mojang.serialization.JsonOps
import io.netty.buffer.ByteBuf
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

data class GeometryModelSynchPayload(
	val nbtMap: Map<ResourceLocation, CompoundTag>
) : ToClientPayload {
	companion object {
		@JvmField
		val TYPE = CustomPacketPayload.Type<GeometryModelSynchPayload>(RcfUtil.modRl("bedrock_model_data_synchs"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, GeometryModelSynchPayload> = StreamCodec.composite(
			RESOURCE_LOCATION_BY_COMPOUND_TAG_MAP_STREAM_CODEC, GeometryModelSynchPayload::nbtMap,
			::GeometryModelSynchPayload
		)
	}

	constructor() : this(GeometryModelRegistry.getInstance(false).getNbtCache())

	override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?> = TYPE

	override fun work(
		context: IPayloadContext,
		player: AbstractClientPlayer
	) {
		val instance = GeometryModelRegistry.getInstance(true)
		instance.clearNbtCache()
		instance.apply(nbtMap.mapValues {
			NbtOps.INSTANCE.convertTo(JsonOps.COMPRESSED, it.value)
		})
	}
}
