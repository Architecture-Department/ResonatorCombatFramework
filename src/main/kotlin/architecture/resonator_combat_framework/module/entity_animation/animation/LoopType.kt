package architecture.resonator_combat_framework.module.entity_animation.animation

import architecture.goldenboughs_lib.util.EnumCodec
import architecture.goldenboughs_lib.util.EnumStreamCodec
import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec

enum class LoopType {
	ONCE,
	LOOP,
	HOLD_ON_LAST;

	companion object {
		@JvmField
		val CODEC: Codec<LoopType> = EnumCodec.create(LoopType::class)

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, LoopType> = EnumStreamCodec.create(LoopType::class)
	}
}