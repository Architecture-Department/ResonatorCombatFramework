package architecture.resonator_combat_framework.init.registry

import architecture.resonator_combat_framework.animation.controller.ActionAnimationController
import architecture.resonator_combat_framework.event.definition.AnimationControllerRegisterEvent
import architecture.resonator_combat_framework.init.RcfAnimationControllers
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

/**
 * 控制器注册事件 —— 在 [AnimationControllerRegisterEvent] 触发时注册各动画控制器。
 */
@EventBusSubscriber(modid = RcfUtil.ID)
object AnimationControllerRegistry {
	@SubscribeEvent
	fun registry(event: AnimationControllerRegisterEvent<*>) {
		event.register(RcfAnimationControllers.BACKGROUND_ACTION, priority = 3000)
		event.register(RcfAnimationControllers.ACTION, ::ActionAnimationController, 4000)
		event.register(RcfAnimationControllers.MAIN, priority = 5000)
		event.register(RcfAnimationControllers.ACTION, priority = 5000)
		event.register(RcfAnimationControllers.COMMAND, priority = 6000)
	}
}
