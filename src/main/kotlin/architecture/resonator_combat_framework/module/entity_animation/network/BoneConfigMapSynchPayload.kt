package architecture.resonator_combat_framework.module.entity_animation.network

import architecture.goldenboughs_lib.api.payload.ToClientPayload
import architecture.goldenboughs_lib.util.LibUtil.RESOURCE_LOCATION_BY_COMPOUND_TAG_MAP_STREAM_CODEC
import architecture.resonator_combat_framework.module.entity_animation.registry.BoneConfigRegistry
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

data class BoneConfigMapSynchPayload(
    val nbtMap: Map<ResourceLocation, CompoundTag>
) : ToClientPayload {
    companion object {
        @JvmField
        val TYPE =
            CustomPacketPayload.Type<BoneConfigMapSynchPayload>(RcfUtil.modRl("proxy_bone_config_data_data_synchs"))

        @JvmField
        val STREAM_CODEC: StreamCodec<ByteBuf, BoneConfigMapSynchPayload> = StreamCodec.composite(
            RESOURCE_LOCATION_BY_COMPOUND_TAG_MAP_STREAM_CODEC, BoneConfigMapSynchPayload::nbtMap,
            ::BoneConfigMapSynchPayload
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
