package architecture.resonator_combat_framework.core;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = ResonatorCombatFramework.ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ResonatorCombatFramework.ID, value = Dist.CLIENT)
public class ResonatorCombatFrameworkClient {
	public ResonatorCombatFrameworkClient(ModContainer container) {
	}

	@SubscribeEvent
	static void onClientSetup(FMLClientSetupEvent event) {
		ResonatorCombatFramework.LOGGER.info("HELLO FROM CLIENT SETUP");
		ResonatorCombatFramework.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
	}
}
