package architecture.resonator_combat_framework.animation

import architecture.goldenboughs_lib.util.EnumCodec
import architecture.goldenboughs_lib.util.EnumStreamCodec
import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec

/**
 * 动画循环类型——控制动画播放到末尾后的行为。
 *
 * @property ONCE 播放一次后停止
 * @property LOOP 循环播放
 * @property HOLD_ON_LAST 保持最后一帧
 */
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
