package architecture.resonator_combat_framework.payload.toc

import architecture.goldenboughs_lib.api.payload.ToClientPayload
import architecture.goldenboughs_lib.util.LibUtil.RESOURCE_LOCATION_BY_COMPOUND_TAG_MAP_STREAM_CODEC
import architecture.resonator_combat_framework.registry.BoneConfigRegistry
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
 * 骨骼配置同步数据包 —— 服务端→客户端。
 * 在数据包同步时将服务端的骨骼配置数据发送给客户端，
 * 包括过渡时间、骨骼标志、时间线等动画行为配置。
 *
 * @property nbtMap 骨骼配置 ID 到 NBT 序列化数据的映射
 */
data class BoneConfigSynchPayload(
	val nbtMap: Map<ResourceLocation, CompoundTag>
) : ToClientPayload {
	companion object {
		@JvmField
		val TYPE =
			CustomPacketPayload.Type<BoneConfigSynchPayload>(RcfUtil.modRl("bone_config_synchs"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, BoneConfigSynchPayload> = StreamCodec.composite(
			RESOURCE_LOCATION_BY_COMPOUND_TAG_MAP_STREAM_CODEC, BoneConfigSynchPayload::nbtMap,
			::BoneConfigSynchPayload
		)
	}

	constructor() : this(BoneConfigRegistry.getInstance(false).getNbtCache())

	override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?> = TYPE

	override fun work(
		context: IPayloadContext,
		player: AbstractClientPlayer
	) {
		val instance = BoneConfigRegistry.getInstance(true)
		instance.clearNbtCache()
		instance.apply(nbtMap.mapValues {
			NbtOps.INSTANCE.convertTo(JsonOps.COMPRESSED, it.value)
		})
	}
}
