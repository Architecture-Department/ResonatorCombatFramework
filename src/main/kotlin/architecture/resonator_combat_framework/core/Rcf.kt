package architecture.resonator_combat_framework.core

import architecture.resonator_combat_framework.core.RcfConstants.LOGGER
import architecture.resonator_combat_framework.init.RcfDataComponentTypes
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.event.server.ServerStartingEvent
import thedarkcolour.kotlinforforge.neoforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(RcfConstants.ID)
@EventBusSubscriber
object Rcf {
	@SubscribeEvent
	fun onServerStarting(event: ServerStartingEvent) {
		LOGGER.info("HELLO from server starting")
	}

	init {
		val modContainer = LOADING_CONTEXT.activeContainer
		val modBus = MOD_BUS
		RcfDataComponentTypes.REGISTRY.register(modBus)
	}
}
