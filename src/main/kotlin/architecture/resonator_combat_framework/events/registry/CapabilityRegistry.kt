package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.core.RcfConstants
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent

/**
 * 注册能力
 */
@EventBusSubscriber(modid = RcfConstants.ID)
object CapabilityRegistry {
	@SubscribeEvent
	fun registry(event: RegisterCapabilitiesEvent?) {
	}
}
