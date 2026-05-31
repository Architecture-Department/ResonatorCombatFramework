package architecture.resonator_combat_framework.module.player_animation.client

import net.minecraft.client.Minecraft
import net.neoforged.fml.loading.FMLLoader

// 客户端
object RcfFirstPersonRender {
	@JvmStatic
	val IRSTPERSON_MOD_LOAD = FMLLoader.getLoadingModList().getModFileById("firstperson") != null

	// TODO 需要在UI中特殊处理
	@JvmStatic
	fun isFirstPersonPass(): Boolean {
		val minecraft = Minecraft.getInstance()
		val player = minecraft.player
		return !IRSTPERSON_MOD_LOAD &&
			!minecraft.gameRenderer.mainCamera.isDetached &&
			player != null &&
			player.`resonator_combat_framework$getAnimationTransformer`().isActive()
	}
}