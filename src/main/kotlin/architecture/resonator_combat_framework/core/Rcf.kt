package architecture.resonator_combat_framework.core

import architecture.resonator_combat_framework.init.RcfDataComponentTypes
import architecture.resonator_combat_framework.module.player_animation.client.RcfPlayerAnimationBridge
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.registries.DeferredRegister
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.annotations.Contract
import thedarkcolour.kotlinforforge.neoforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(Rcf.ID)
@EventBusSubscriber
object Rcf {
	const val ID: String = "resonator_combat_framework"
	const val NAME: String = "ResonatorCombatFramework"

	@JvmField
	val LOGGER: Logger = LogManager.getLogger(ID)

	init {
		val modContainer = LOADING_CONTEXT.activeContainer
		val modBus = MOD_BUS
		RcfDataComponentTypes.REGISTRY.register(modBus)
		RcfPlayerAnimationBridge.register()
	}

	@SubscribeEvent
	fun onServerStarting(event: ServerStartingEvent) {
		LOGGER.info("HELLO from server starting")
	}

	@JvmStatic
	@Contract("_ -> new")
	fun modRl(name: String): ResourceLocation {
		return ResourceLocation.fromNamespaceAndPath(ID, name)
	}

	@JvmStatic
	@Contract(pure = true)
	fun modRlText(name: String): String {
		return "$ID:$name"
	}

	@JvmStatic
	fun <T> modRegister(registry: Registry<T>): DeferredRegister<T> {
		return DeferredRegister.create<T>(registry, ID)
	}

	@JvmStatic
	fun <T> modRegister(registry: ResourceKey<Registry<T>>): DeferredRegister<T> {
		return DeferredRegister.create<T>(registry, ID)
	}

	@JvmStatic
	fun getSparkModuleRl(namespace: String, typeName: String, path: String): ResourceLocation {
		return ResourceLocation.fromNamespaceAndPath(namespace, "spark_modules/$typeName/$path")
	}
}
