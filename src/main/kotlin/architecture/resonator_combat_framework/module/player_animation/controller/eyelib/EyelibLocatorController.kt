package architecture.resonator_combat_framework.module.player_animation.controller.eyelib

import architecture.resonator_combat_framework.module.player_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.player_animation.api.ProxyLocator
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos

/**
 * Locator 控制器。将 eyelib 动画中的 Locator 骨骼数据写入对应父骨骼的 locator 映射。
 *
 * @param locatorParents Locator 名称 → 父骨骼名称的映射，
 *   例如 mapOf("right_item" to "right_arm", "left_item" to "left_arm")
 */
class EyelibLocatorController(
	private val locatorParents: Map<String, String> = emptyMap()
) {
	/**
	 * 从 BoneRenderInfos 读取 Locator 数据，写入 proxyModel 中对应父骨骼的 locator。
	 * 如果父骨骼尚不存在则先创建。
	 */
	fun writeLocators(infos: BoneRenderInfos, proxyModel: ProxyModel) {
		for ((locatorName, parentName) in locatorParents) {
			val boneId = GlobalBoneIdHandler.get(locatorName) ?: continue
			if (!infos.infos.containsKey(boneId)) continue
			val info = infos.getData(boneId)

			val parentBone = proxyModel.getBone(parentName)
				?: ProxyBone(parentName).also { proxyModel.addBone(it) }

			val locator = parentBone.getLocator(locatorName)
				?: ProxyLocator(locatorName).also { parentBone.addLocator(it) }

			locator.pos.set(info.renderPosition)
			locator.rotation.set(info.renderRotation)
			locator.scale.set(info.renderScala)
		}
	}
}
