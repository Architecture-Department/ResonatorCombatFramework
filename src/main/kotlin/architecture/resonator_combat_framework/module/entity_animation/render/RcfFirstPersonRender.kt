package architecture.resonator_combat_framework.module.entity_animation.render

import architecture.resonator_combat_framework.core.Rcf.IRSTPERSON_LOADED
import net.minecraft.client.Minecraft

object RcfFirstPersonRender {
	// TODO 需要在UI中特殊处理
	@JvmStatic
	fun isFirstPersonPass(): Boolean {
		val minecraft = Minecraft.getInstance()
		val player = minecraft.player
		return !IRSTPERSON_LOADED &&
			!minecraft.gameRenderer.mainCamera.isDetached &&
			player != null &&
			player.`resonator_combat_framework$getAnimationTransformer`().isActive()
	}
}

