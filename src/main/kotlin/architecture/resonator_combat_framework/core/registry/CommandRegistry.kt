package architecture.resonator_combat_framework.core.registry

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.command.TestAnimCommand
import architecture.resonator_combat_framework.module.player_animation.command.TestAnimStopCommand
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.RegisterCommandsEvent
import architecture.resonator_combat_framework.core.RcfConstants

@EventBusSubscriber(modid = RcfConstants.ID)
object CommandRegistry {
	@SubscribeEvent
	fun registry(event: RegisterCommandsEvent) {
		TestAnimCommand.register(event.dispatcher)
		TestAnimStopCommand.register(event.dispatcher)
	}
}
