package architecture.resonator_combat_framework.core

import architecture.resonator_combat_framework.config.RcfConfig
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.init.RcfDataComponentTypes
import architecture.resonator_combat_framework.util.RcfUtil
import architecture.resonator_combat_framework.util.RcfUtil.LOGGER
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.event.server.ServerStartingEvent
import thedarkcolour.kotlinforforge.neoforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(RcfUtil.ID)
@EventBusSubscriber
object Rcf {
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
