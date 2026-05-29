package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.core.RcfConstants.modRegister
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

object RcfDataComponentTypes {
	@JvmField
	val REGISTRY: DeferredRegister<DataComponentType<*>> =
		modRegister<DataComponentType<*>>(BuiltInRegistries.DATA_COMPONENT_TYPE)

	@JvmField
	val STACK_ANIMATABLE_ID_COMPONENT: Supplier<DataComponentType<Long>> = register<Long>(
		"stack_animatable_id",
		Codec.LONG, ByteBufCodecs.VAR_LONG, false
	)

	private fun recordBoolean(name: String, isCacheEncoding: Boolean): Supplier<DataComponentType<Boolean>> {
		return register<Boolean>(name, Codec.BOOL, ByteBufCodecs.BOOL, isCacheEncoding)
	}

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

	private fun <T> register(
		name: String,
		builder: UnaryOperator<DataComponentType.Builder<T>>
	): Supplier<DataComponentType<T>> {
		return register<DataComponentType<T>>(
			name,
			Supplier { builder.apply(DataComponentType.builder<T>()).build() })
	}

	private fun <B : DataComponentType<*>> register(
		name: String,
		builder: Supplier<out B>
	): DeferredHolder<DataComponentType<*>, B> {
		return REGISTRY.register<B>("data_components.$name", builder)
	}

	private fun recordString(name: String, isCacheEncoding: Boolean): Supplier<DataComponentType<String>> {
		return register<String>(name, Codec.STRING, ByteBufCodecs.STRING_UTF8, isCacheEncoding)
	}
}
