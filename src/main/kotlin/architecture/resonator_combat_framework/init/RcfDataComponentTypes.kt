package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.util.RcfUtil.modRegister
import com.mojang.serialization.Codec
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier
import java.util.function.UnaryOperator

/**
 * RCF 数据组件类型注册 —— 注册物品/方块的自定义数据组件。
 * 当前预留了注册框架，尚未注册具体组件。
 */
object RcfDataComponentTypes {
	@JvmField
	val REGISTRY: DeferredRegister<DataComponentType<*>> =
		modRegister<DataComponentType<*>>(BuiltInRegistries.DATA_COMPONENT_TYPE)

	/** 注册布尔类型数据组件 */
	private fun recordBoolean(name: String, isCacheEncoding: Boolean): Supplier<DataComponentType<Boolean>> {
		return register<Boolean>(name, Codec.BOOL, ByteBufCodecs.BOOL, isCacheEncoding)
	}

	/** 注册带编解码器和流编解码器的数据组件 */
	private fun <T> register(
		name: String,
		codec: Codec<T>,
		streamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>,
		isCacheEncoding: Boolean
	): Supplier<DataComponentType<T>> {
		return register(name) { builder: DataComponentType.Builder<T> ->
			builder.persistent(codec).networkSynchronized(streamCodec)
			if (isCacheEncoding) {
				builder.cacheEncoding()
			}
			builder
		}
	}

	/** 注册自定义构建器的数据组件 */
	private fun <T> register(
		name: String,
		builder: UnaryOperator<DataComponentType.Builder<T>>
	): Supplier<DataComponentType<T>> {
		return register<DataComponentType<T>>(
			name,
			Supplier { builder.apply(DataComponentType.builder<T>()).build() })
	}

	/** 内部注册方法 —— 将组件注册到 DeferredRegister */
	private fun <B : DataComponentType<*>> register(
		name: String,
		builder: Supplier<out B>
	): DeferredHolder<DataComponentType<*>, B> {
		return REGISTRY.register<B>("data_components.$name", builder)
	}

	/** 注册字符串类型数据组件 */
	private fun recordString(name: String, isCacheEncoding: Boolean): Supplier<DataComponentType<String>> {
		return register<String>(name, Codec.STRING, ByteBufCodecs.STRING_UTF8, isCacheEncoding)
	}
}
