package architecture.resonator_combat_framework.events.registry

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.animation_controller.ActionAnimationController
import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.player_animation.event.AnimationControllerRegisterEvent
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

@EventBusSubscriber(modid = RcfConstants.ID)
object AnimationControllerRegistry {
	@JvmField
	val ACTION: ResourceLocation = rlOf(RcfConstants.ID, "action")

	@JvmField
	val MAIN: ResourceLocation = rlOf(RcfConstants.ID, "main")

	@JvmField
	val COMMAND: ResourceLocation = rlOf(RcfConstants.ID, "command")

	@SubscribeEvent
	fun registry(event: AnimationControllerRegisterEvent) {
		event.register(ACTION, ::ActionAnimationController, 1000)
		event.register(MAIN, priority = 0)
		event.register(COMMAND, priority = -1000)
	}
}
