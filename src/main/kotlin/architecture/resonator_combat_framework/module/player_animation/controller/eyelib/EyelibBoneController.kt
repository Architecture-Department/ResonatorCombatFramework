package architecture.resonator_combat_framework.module.player_animation.controller.eyelib

import architecture.resonator_combat_framework.module.player_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.controller.IBoneController
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos

/** eyelib 骨骼数据写入 ProxyModel */
class EyelibBoneController : IBoneController<BoneRenderInfos, ProxyModel> {

	override fun writeToProxy(infos: BoneRenderInfos, proxyModel: ProxyModel) {
		for ((boneId, info) in infos.infos) {
			val name = GlobalBoneIdHandler.get(boneId) ?: continue
			val bone = proxyModel.getBone(name) ?: ProxyBone(name).also { proxyModel.addBone(it) }
			bone.pos.set(info.renderPosition)
			bone.rotation.set(info.renderRotation)
			bone.scale.set(info.renderScala)
		}
	}
}
