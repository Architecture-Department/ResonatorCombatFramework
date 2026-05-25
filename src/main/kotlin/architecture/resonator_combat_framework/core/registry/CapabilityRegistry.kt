package architecture.resonator_combat_framework.core.registry

import architecture.resonator_combat_framework.core.Rcf
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import architecture.resonator_combat_framework.core.RcfConstants

/**
 * 注册能力
 */
@EventBusSubscriber(modid = RcfConstants.ID)
object CapabilityRegistry {
	@SubscribeEvent
	fun registerHighest(event: RegisterCapabilitiesEvent?) {
	}
}
