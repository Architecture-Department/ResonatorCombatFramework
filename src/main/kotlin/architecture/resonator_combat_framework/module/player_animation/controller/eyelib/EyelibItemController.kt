package architecture.resonator_combat_framework.module.player_animation.controller.eyelib

import architecture.resonator_combat_framework.module.player_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.player_animation.api.ProxyLocator
import architecture.resonator_combat_framework.module.player_animation.controller.IItemController
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos

/** eyelib 物品控制器 — 从 BoneRenderInfos 提取 right_item/left_item 定位器写入手臂骨骼 */
class EyelibItemController : IItemController<BoneRenderInfos, ProxyBone> {

	override fun writeToProxy(infos: BoneRenderInfos, leftArm: ProxyBone, rightArm: ProxyBone) {
		writeLocator(infos, "right_item", rightArm)
		writeLocator(infos, "left_item", leftArm)
	}

	private fun writeLocator(infos: BoneRenderInfos, boneName: String, armBone: ProxyBone) {
		val id = GlobalBoneIdHandler.get(boneName)
		val locator: ProxyLocator = armBone.getLocator(boneName) ?: run {
			val locator = ProxyLocator(boneName)
			armBone.addLocator(locator)
			return@run locator
		}
		if (!infos.infos.containsKey(id)) {
			locator.pos.set(0f); locator.rotation.set(0f)
			return
		}
		val info = infos.getData(id)
		locator.pos.set(info.renderPosition)
		locator.rotation.set(info.renderRotation)
	}
}
