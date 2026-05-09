package architecture.resonator_combat_framework.events;

import architecture.goldenboughs_lib.core.GoldenBoughsLib;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = GoldenBoughsLib.ID)
public final class EntityEvets {
	@SubscribeEvent
	public void entity$Pre(EntityTickEvent.Pre pre) {
		Entity entity = pre.getEntity();
		entity.allTick();
		entity.allAnimTick();
		if (entity.isSpectator()) {
			entity.allStopAllAnimation();
		}
	}
}
