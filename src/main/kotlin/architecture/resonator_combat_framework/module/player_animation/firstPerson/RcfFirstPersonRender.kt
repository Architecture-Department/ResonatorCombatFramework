package architecture.resonator_combat_framework.module.player_animation.firstPerson

import net.minecraft.client.Minecraft

// 客户端
object RcfFirstPersonRender {
	@JvmStatic
	fun isFirstPersonPass(): Boolean =
		!Minecraft.getInstance().gameRenderer.mainCamera.isDetached &&
			Minecraft.getInstance().player!!
				.`resonator_combat_framework$getAnimationTransformer`()
				.isActive()

}
