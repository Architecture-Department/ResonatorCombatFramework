package architecture.resonator_combat_framework.events

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.core.Rcf.modRl
import architecture.resonator_combat_framework.event.PlayerControllerEvent
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import software.bernie.geckolib.animation.PlayState

@EventBusSubscriber(modid = Rcf.ID)
object PlayerControllerEvents {
	@JvmField
	val BASE_CONTROLLER: ResourceLocation = ResourceLocation.parse("base_controller")

	@JvmField
	val UPPER_BODY: ResourceLocation = modRl("upper_body")

	@JvmField
	val LOWER_BODY: ResourceLocation = modRl("lower_body")

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	fun registered(evets: PlayerControllerEvent.Register) {
		evets.register(BASE_CONTROLLER, 2) { PlayState.STOP }
		evets.register(UPPER_BODY, 2) { PlayState.STOP }
		evets.register(LOWER_BODY, 2) { PlayState.STOP }
	}
}