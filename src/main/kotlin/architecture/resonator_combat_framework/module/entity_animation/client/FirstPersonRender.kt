package architecture.resonator_combat_framework.module.entity_animation.client

import architecture.resonator_combat_framework.module.entity_animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.client.Minecraft

/**
 * 第一人称渲染判断工具类。
 * 用于判断当前是否处于第一人称动画渲染阶段，
 * 在 UI 等特殊处理场景中用于决定是否隐藏或调整玩家模型的渲染。
 */
object FirstPersonRender {
	// TODO 需要在UI中特殊处理
	/**
	 * 判断当前是否处于第一人称动画渲染通道。
	 * 当满足以下条件时返回 true：
	 * - 未加载 IRSTPerson 模组
	 * - 相机未分离（不是第三人称或旁观模式）
	 * - 玩家存在
	 * - 玩家的动画映射器处于活跃状态
	 *
	 * @return 是否处于第一人称动画渲染阶段
	 */
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
