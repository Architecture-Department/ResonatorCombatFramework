package architecture.resonator_combat_framework.config

import architecture.goldenboughs_lib.api.BasicConfigMapper
import architecture.goldenboughs_lib.util.LibUtil
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.neoforge.common.ModConfigSpec

@EventBusSubscriber(modid = RcfUtil.ID)
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
		RcfUtil.LOGGER.info("Initialize the ${LibUtil.NAME} config files")
		modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC)
	}

	@SubscribeEvent
	fun onLoad(configEvent: ModConfigEvent.Loading) {
		RcfUtil.LOGGER.info("Loaded ${LibUtil.NAME} config file ${configEvent.config.fileName}")
	}

	@SubscribeEvent
	fun onFileChange(configEvent: ModConfigEvent.Reloading) {
		RcfUtil.LOGGER.info("${LibUtil.NAME} config just got changed on the file system!")
	}
}