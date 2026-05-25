package architecture.resonator_combat_framework.module.player_animation

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.core.Rcf
import io.github.tt432.eyelib.Eyelib
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.capability.component.ModelComponent
import architecture.resonator_combat_framework.core.RcfConstants

object PlayerAnimationSetup {

	private val modelInfo = ModelComponent.SerializableInfo(
		// 注：模型id是模型中的identifier决定的不通过路径或其他
		RcfConstants.modRlText("player_proxy"),
		RcfConstants.modRl("empty"),
		rlOf(Eyelib.MOD_ID, "entity_cutout_no_cull")
	)

	fun setupRenderData(renderData: RenderData<*>) {
		renderData.isUseBuiltInRenderSystem = false

		val modelComponent = ModelComponent()
		modelComponent.setInfo(modelInfo)
		renderData.modelComponents.clear()
		renderData.modelComponents.add(modelComponent)
	}
}
