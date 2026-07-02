package architecture.resonator_combat_framework.events.client

import architecture.resonator_combat_framework.module.entity_animation.client.FirstPersonRender
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderArmEvent
import net.neoforged.neoforge.client.event.RenderHandEvent

@EventBusSubscriber(modid = RcfUtil.ID, value = [Dist.CLIENT])
object RendererEvets {
	/**
	 * 取消手部渲染：在第一人称渲染通道时隐藏原版手部模型。
	 */
	@SubscribeEvent
	fun onRenderHand(event: RenderHandEvent) {
		if (FirstPersonRender.isFirstPersonPass()) {
			event.isCanceled = true
		}
	}

	@SubscribeEvent
	fun onRenderArm(event: RenderArmEvent) {
		val minecraft = Minecraft.getInstance()
		if (FirstPersonRender.isFirstPersonPass() && event.player == minecraft.player) {
			event.isCanceled = true
		}
	}
}
