package architecture.resonator_combat_framework.config

import architecture.goldenboughs_lib.api.BasicConfigMapper
import architecture.goldenboughs_lib.core.LibConstants
import architecture.resonator_combat_framework.core.RcfConstants
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.neoforge.common.ModConfigSpec

@EventBusSubscriber(modid = RcfConstants.ID)
object RcfConfig : BasicConfigMapper() {

	@JvmField
	val CLIENT: RcfClientConfig

	@JvmField
	val CLIENT_SPEC: ModConfigSpec

	init {
		val clientPair = configure(::RcfClientConfig)
		CLIENT = clientPair.getLeft()
		CLIENT_SPEC = clientPair.getRight()
	}

	@JvmStatic
	fun register(modContainer: ModContainer) {
		RcfConstants.LOGGER.info("Initialize the ${LibConstants.NAME} config files")
		modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC)
	}

	@SubscribeEvent
	fun onLoad(configEvent: ModConfigEvent.Loading) {
		RcfConstants.LOGGER.info("Loaded ${LibConstants.NAME} config file ${configEvent.config.fileName}")
	}

	@SubscribeEvent
	fun onFileChange(configEvent: ModConfigEvent.Reloading) {
		RcfConstants.LOGGER.info("${LibConstants.NAME} config just got changed on the file system!")
	}
}