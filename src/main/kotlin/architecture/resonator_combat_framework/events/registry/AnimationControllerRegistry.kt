package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.ActionAnimationController
import architecture.resonator_combat_framework.module.entity_animation.event.AnimationControllerRegisterEvent
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

object AnimationControllers {
	@JvmField
	val BACKGROUND_ACTION: ResourceLocation = RcfConstants.modRl("background_action")

	@JvmField
	val ACTION: ResourceLocation = RcfConstants.modRl("action")

	@JvmField
	val MAIN: ResourceLocation = RcfConstants.modRl("main")

	@JvmField
	val COMMAND: ResourceLocation = RcfConstants.modRl("command")
}

@EventBusSubscriber(modid = RcfConstants.ID)
object AnimationControllerRegistry {
	@SubscribeEvent
	fun registry(event: AnimationControllerRegisterEvent<*>) {
		event.register(AnimationControllers.BACKGROUND_ACTION, priority = 3000)
		event.register(AnimationControllers.ACTION, ::ActionAnimationController, 4000)
		event.register(AnimationControllers.MAIN, priority = 5000)
		event.register(AnimationControllers.COMMAND, priority = 6000)
	}
}
