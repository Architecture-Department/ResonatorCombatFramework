package architecture.resonator_combat_framework.core.registry

import architecture.resonator_combat_framework.core.Rcf
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.AddReloadListenerEvent
import software.bernie.geckolib.cache.GeckoLibCache


@EventBusSubscriber(modid = Rcf.ID)
object DataPacketRegistry {
	@SubscribeEvent
	fun register(event: AddReloadListenerEvent) {
		event.addListener(GeckoLibCache::reload)
	}
}