package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.player_animation.controller.BedrockAnimationController
import architecture.resonator_combat_framework.module.player_animation.event.AnimationControllerRegisterEvent
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

@EventBusSubscriber(modid = RcfConstants.ID)
object AnimationControllerRegistry {
	const val DEFAULT: String = "default"

	@SubscribeEvent
	fun registry(event: AnimationControllerRegisterEvent) {
		event.register(DEFAULT, { BedrockAnimationController(it) })
	}
}