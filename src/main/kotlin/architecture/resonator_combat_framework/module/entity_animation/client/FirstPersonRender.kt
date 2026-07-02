package architecture.resonator_combat_framework.module.entity_animation.client

import architecture.resonator_combat_framework.module.entity_animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.client.Minecraft

object FirstPersonRender {
	// TODO 需要在UI中特殊处理
	@JvmStatic
	fun isFirstPersonPass(): Boolean {
		val minecraft = Minecraft.getInstance()
		val player = minecraft.player
		return !RcfUtil.IRSTPERSON_LOADED &&
			!minecraft.gameRenderer.mainCamera.isDetached &&
			player != null &&
			player.getMapperProvider().isActive()
	}
}