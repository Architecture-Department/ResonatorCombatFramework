package architecture.resonator_combat_framework.module.player_animation

import net.minecraft.client.Minecraft
import net.neoforged.fml.loading.FMLLoader

// 客户端
object RcfFirstPersonRender {
	@JvmStatic
	val IRSTPERSON_MOD_LOAD = FMLLoader.getLoadingModList().getModFileById("firstperson") != null

	@JvmStatic
	fun isFirstPersonPass(): Boolean =
		!IRSTPERSON_MOD_LOAD &&
			!Minecraft.getInstance().gameRenderer.mainCamera.isDetached &&
			Minecraft.getInstance().player!!.`resonator_combat_framework$getAnimationTransformer`().isActive()
}