package architecture.resonator_combat_framework.core.registry;

import architecture.resonator_combat_framework.core.Rcf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * 注册能力
 */
@EventBusSubscriber(modid = Rcf.ID)
public final class CapabilityRegistry {

	@SubscribeEvent
	public static void registerHighest(RegisterCapabilitiesEvent event) {
	}
}
