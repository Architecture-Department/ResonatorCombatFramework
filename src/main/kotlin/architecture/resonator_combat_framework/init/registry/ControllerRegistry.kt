package architecture.resonator_combat_framework.init.registry

import architecture.resonator_combat_framework.animation.controller.ActionAnimationController
import architecture.resonator_combat_framework.event.ControllerRegisterEvent
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

/**
 * 动画控制器 ID 定义 —— 集中管理所有动画控制器的注册键。
 */
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

/**
 * 控制器注册事件 —— 在 [ControllerRegisterEvent] 触发时注册各动画控制器。
 */
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
