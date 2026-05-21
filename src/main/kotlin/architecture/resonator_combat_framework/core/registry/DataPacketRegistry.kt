package architecture.resonator_combat_framework.core.registry

import architecture.resonator_combat_framework.core.Rcf
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.AddReloadListenerEvent
import software.bernie.geckolib.cache.GeckoLibCache

@EventBusSubscriber(modid = Rcf.ID, value = [Dist.DEDICATED_SERVER])
object DataPacketRegistry {
	@SubscribeEvent
	fun register(event: AddReloadListenerEvent) {
		event.addListener(GeckoLibCache::reload)
	}
}

@EventBusSubscriber(modid = Rcf.ID, value = [Dist.CLIENT])
object DataPacketRegistryClient {
	@SubscribeEvent
	fun register(event: AddReloadListenerEvent) {
	}
}