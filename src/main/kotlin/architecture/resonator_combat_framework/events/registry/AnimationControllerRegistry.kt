package architecture.resonator_combat_framework.events.registry

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.player_animation.event.AnimationControllerRegisterEvent
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

@EventBusSubscriber(modid = RcfConstants.ID)
object AnimationControllerRegistry {
	//	附加层
	@JvmField
	val ADDON: ResourceLocation = rlOf(RcfConstants.ID, "addon")

	// 默认控制器
	@JvmField
	val DEFAULT: ResourceLocation = rlOf(RcfConstants.ID, "default")

	//	下半身
	@JvmField
	val LOWER_BODY: ResourceLocation = rlOf(RcfConstants.ID, "lower_body")

	//	上半身
	@JvmField
	val UPPER_BODY: ResourceLocation = rlOf(RcfConstants.ID, "upper_body")

	@SubscribeEvent
	fun registry(event: AnimationControllerRegisterEvent) {
		event.register(ADDON, priority = 2000)
		event.register(DEFAULT, priority = 1000)
		event.register(LOWER_BODY, priority = 500)
		event.register(UPPER_BODY, priority = 400)
	}
}