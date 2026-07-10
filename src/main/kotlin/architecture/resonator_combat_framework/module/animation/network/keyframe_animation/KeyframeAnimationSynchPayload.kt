package architecture.resonator_combat_framework.module.animation.network.keyframe_animation

import architecture.goldenboughs_lib.api.payload.ToClientPayload
import architecture.goldenboughs_lib.util.LibUtil.RESOURCE_LOCATION_BY_COMPOUND_TAG_MAP_STREAM_CODEC
import architecture.resonator_combat_framework.module.animation.registry.KeyframeAnimationRegistry
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
 * 关键帧动画同步数据包 —— 服务端→客户端。
 * 在数据包同步（[OnDatapackSyncEvent]）时将服务端的关键帧动画数据发送给客户端，
 * 保证客户端在资源重载后获取最新的动画定义。
 *
 * @property nbtMap 动画 ID 到 NBT 序列化数据的映射
 */
data class KeyframeAnimationSynchPayload(
	val nbtMap: Map<ResourceLocation, CompoundTag>
) : ToClientPayload {
	companion object {
		@JvmField
		val TYPE =
			CustomPacketPayload.Type<KeyframeAnimationSynchPayload>(RcfUtil.modRl("keyframe_animation_synchs"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, KeyframeAnimationSynchPayload> = StreamCodec.composite(
			RESOURCE_LOCATION_BY_COMPOUND_TAG_MAP_STREAM_CODEC, KeyframeAnimationSynchPayload::nbtMap,
			::KeyframeAnimationSynchPayload
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
