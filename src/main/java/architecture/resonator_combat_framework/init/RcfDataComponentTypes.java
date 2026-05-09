package architecture.resonator_combat_framework.init;

import architecture.goldenboughs_lib.init.LibDataComponentTypes;
import architecture.resonator_combat_framework.core.Rcf;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class RcfDataComponentTypes {
	public static final DeferredRegister<DataComponentType<?>> REGISTRY = Rcf.modRegister(BuiltInRegistries.DATA_COMPONENT_TYPE);

	public static final Supplier<DataComponentType<Long>> STACK_ANIMATABLE_ID_COMPONENT = register("stack_animatable_id",
		Codec.LONG, ByteBufCodecs.VAR_LONG, false);

	private static Supplier<DataComponentType<Boolean>> recordBoolean(String name, boolean isCacheEncoding) {
		return register(name, Codec.BOOL, ByteBufCodecs.BOOL, isCacheEncoding);
	}

	private static <T> Supplier<DataComponentType<T>> register(String name, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec, boolean isCacheEncoding) {
		return register(name, builder -> {
			builder.persistent(codec).networkSynchronized(streamCodec);
			if (isCacheEncoding) {
				builder.cacheEncoding();
			}
			return builder;
		});
	}

	private static <T> Supplier<DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
		return register(name, () -> builder.apply(DataComponentType.builder()).build());
	}

	private static <B extends DataComponentType<?>> DeferredHolder<DataComponentType<?>, B> register(String name, Supplier<? extends B> builder) {
		return LibDataComponentTypes.REGISTRY.register("data_components." + name, builder);
	}

	private static Supplier<DataComponentType<String>> recordString(String name, boolean isCacheEncoding) {
		return register(name, Codec.STRING, ByteBufCodecs.STRING_UTF8, isCacheEncoding);
	}
}
