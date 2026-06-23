package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.module.entity_animation.command.TestAnimCommand
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.RegisterCommandsEvent

@EventBusSubscriber(modid = RcfUtil.ID)
object CommandRegistry {
	@SubscribeEvent
	fun registry(event: RegisterCommandsEvent) {
		TestAnimCommand.register(event.dispatcher)
	}
}
