package architecture.resonator_combat_framework.core.registry

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.command.GeoAnimatableCommand
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.RegisterCommandsEvent

/**
 * 指令事件
 */
@EventBusSubscriber(modid = Rcf.ID)
object CommandRegistry {
	@SubscribeEvent
	fun registry(event: RegisterCommandsEvent) {
		val dispatcher = event.dispatcher
		GeoAnimatableCommand.registry(dispatcher)
	}
}
