package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.init.RcfRegistries
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.registries.NewRegistryEvent

@EventBusSubscriber(modid = RcfUtil.ID)
object RegistriesRegistry {
	@SubscribeEvent
	fun register(event: NewRegistryEvent) {
		RcfRegistries.register(event)
	}
}