package architecture.resonator_combat_framework.core

import architecture.resonator_combat_framework.config.RcfConfig
import architecture.resonator_combat_framework.core.RcfConstants.LOGGER
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.init.RcfDataComponentTypes
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.LoadingModList
import net.neoforged.neoforge.event.server.ServerStartingEvent
import thedarkcolour.kotlinforforge.neoforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(RcfConstants.ID)
@EventBusSubscriber
object Rcf {
	@JvmStatic
	val IRSTPERSON_LOADED = LoadingModList.get().getModFileById("firstperson") != null

	@JvmStatic
	val GECKOLIB_LOADED = LoadingModList.get().getModFileById("geckolib") != null

	@JvmStatic
	val PARTICLESTORM_LOADED = LoadingModList.get().getModFileById("geckolib") != null

	@SubscribeEvent
	fun onServerStarting(event: ServerStartingEvent) {
		LOGGER.info("HELLO from server starting")
	}

	init {
		val modContainer = LOADING_CONTEXT.activeContainer
		val modBus = MOD_BUS
		RcfConfig.register(modContainer)
		RcfAttachmentTypes.REGISTRY.register(modBus)
		RcfDataComponentTypes.REGISTRY.register(modBus)
	}
}
