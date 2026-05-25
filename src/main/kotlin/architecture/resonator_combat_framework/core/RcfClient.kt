package architecture.resonator_combat_framework.core

import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.neoforge.forge.LOADING_CONTEXT
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import architecture.resonator_combat_framework.core.RcfConstants

@Mod(value = RcfConstants.ID, dist = [Dist.CLIENT])
@EventBusSubscriber(modid = RcfConstants.ID, value = [Dist.CLIENT])
object RcfClient {
	init {
		val modContainer = LOADING_CONTEXT.activeContainer
		val modBus = MOD_BUS
	}

	@SubscribeEvent
	fun onClientSetup(event: FMLClientSetupEvent) {
		RcfConstants.LOGGER.info("HELLO FROM CLIENT SETUP")
		RcfConstants.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().user.name)
	}
}
