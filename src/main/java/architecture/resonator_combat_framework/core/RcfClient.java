package architecture.resonator_combat_framework.core;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = Rcf.ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Rcf.ID, value = Dist.CLIENT)
public class RcfClient {
	public RcfClient(ModContainer container) {
	}

	@SubscribeEvent
	static void onClientSetup(FMLClientSetupEvent event) {
		Rcf.LOGGER.info("HELLO FROM CLIENT SETUP");
		Rcf.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
	}
}
