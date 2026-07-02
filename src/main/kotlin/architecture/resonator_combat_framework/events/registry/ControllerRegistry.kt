package architecture.resonator_combat_framework.events.registry

import architecture.resonator_combat_framework.animation.controller.ActionAnimationController
import architecture.resonator_combat_framework.module.entity_animation.event.ControllerRegisterEvent
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

object AnimationControllers {
	@JvmField
	val BACKGROUND_ACTION: ResourceLocation = RcfUtil.modRl("background_action")

	@JvmField
	val ACTION: ResourceLocation = RcfUtil.modRl("action")

	@JvmField
	val MAIN: ResourceLocation = RcfUtil.modRl("main")

	@JvmField
	val COMMAND: ResourceLocation = RcfUtil.modRl("command")
}

@EventBusSubscriber(modid = RcfUtil.ID)
object ControllerRegistry {
	@SubscribeEvent
	fun registry(event: ControllerRegisterEvent<*>) {
		event.register(AnimationControllers.BACKGROUND_ACTION, priority = 3000)
		event.register(AnimationControllers.ACTION, ::ActionAnimationController, 4000)
		event.register(AnimationControllers.MAIN, priority = 5000)
		event.register(AnimationControllers.COMMAND, priority = 6000)
	}
}
