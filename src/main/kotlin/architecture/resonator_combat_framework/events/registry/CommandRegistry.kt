package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.command.TestAnimCommand
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.RegisterCommandsEvent

@EventBusSubscriber(modid = RcfConstants.ID)
object CommandRegistry {
	@SubscribeEvent
	fun registry(event: RegisterCommandsEvent) {
		TestAnimCommand.register(event.dispatcher)
	}
}
