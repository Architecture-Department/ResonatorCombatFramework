package architecture.resonator_combat_framework.module.animation.network.keyframe_animation

import architecture.goldenboughs_lib.api.payload.ToClientPayload
import architecture.goldenboughs_lib.util.LibUtil.RESOURCE_LOCATION_BY_COMPOUND_TAG_MAP_STREAM_CODEC
import architecture.resonator_combat_framework.module.animation.registry.GeometryModelRegistry
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

/**
 * 几何模型同步数据包 —— 服务端→客户端。
 * 在数据包同步时将服务端的几何模型数据发送给客户端，
 * 包括骨骼结构、定位器、立方体定义等 Bedrock 几何模型信息。
 *
 * @property nbtMap 模型 ID 到 NBT 序列化数据的映射
 */
data class GeometryModelSynchPayload(
	val nbtMap: Map<ResourceLocation, CompoundTag>
) : ToClientPayload {
	companion object {
		@JvmField
		val TYPE = CustomPacketPayload.Type<GeometryModelSynchPayload>(RcfUtil.modRl("geometry_model_synch"))

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
