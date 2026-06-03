package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.controller.ActionAnimationController
import architecture.resonator_combat_framework.module.entity_animation.event.AnimationControllerRegisterEvent
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

@EventBusSubscriber(modid = RcfConstants.ID)
object AnimationControllerRegistry {
	@JvmField
	val BACKGROUND_ACTION: ResourceLocation = RcfConstants.modRl("background_action")

	@JvmField
	val ACTION: ResourceLocation = RcfConstants.modRl("action")

	@JvmField
	val MAIN: ResourceLocation = RcfConstants.modRl("main")

	@JvmField
	val COMMAND: ResourceLocation = RcfConstants.modRl("command")

	@SubscribeEvent
	fun registry(event: AnimationControllerRegisterEvent) {
		event.register(BACKGROUND_ACTION, priority = 3000)
		event.register(ACTION, ::ActionAnimationController, 4000)
		event.register(MAIN, priority = 5000)
		event.register(COMMAND, priority = 6000)
	}
}

