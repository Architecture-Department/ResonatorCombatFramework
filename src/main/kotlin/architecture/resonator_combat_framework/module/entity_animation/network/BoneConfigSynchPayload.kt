package architecture.resonator_combat_framework.module.entity_animation.network

import architecture.goldenboughs_lib.api.payload.ToClientPayload
import architecture.goldenboughs_lib.util.LibUtil.RESOURCE_LOCATION_BY_COMPOUND_TAG_MAP_STREAM_CODEC
import architecture.resonator_combat_framework.module.entity_animation.registry.KeyframeAnimationRegistry
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
 * 骨骼配置同步数据包（服务端→客户端）。
 * 将服务端 [KeyframeAnimationRegistry] 的 NBT 缓存数据发送到客户端，
 * 客户端接收后解析并更新本地的关键帧动画注册表。
 * 注意：类名虽为 BoneConfigSynchPayload，实际同步的是关键帧动画数据。
 *
 * @property nbtMap 资源位置到 NBT 数据的映射
 */
data class BoneConfigSynchPayload(
	val nbtMap: Map<ResourceLocation, CompoundTag>
) : ToClientPayload {
	companion object {
		@JvmField
		val TYPE =
			CustomPacketPayload.Type<BoneConfigSynchPayload>(RcfUtil.modRl("bedrock_animation_data_synchs"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, BoneConfigSynchPayload> = StreamCodec.composite(
			RESOURCE_LOCATION_BY_COMPOUND_TAG_MAP_STREAM_CODEC, BoneConfigSynchPayload::nbtMap,
			::BoneConfigSynchPayload
		)
	}

	constructor() : this(KeyframeAnimationRegistry.getInstance(false).getNbtCache())

	override fun type(): CustomPacketPayload.Type<out CustomPacketPayload?> = TYPE

	override fun work(
		context: IPayloadContext,
		player: AbstractClientPlayer
	) {
		val instance = KeyframeAnimationRegistry.getInstance(true)
		instance.clearNbtCache()
		instance.apply(nbtMap.mapValues {
			NbtOps.INSTANCE.convertTo(JsonOps.COMPRESSED, it.value)
		})
	}
}
