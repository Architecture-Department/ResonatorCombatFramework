package architecture.resonator_combat_framework.module.player_animation

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.core.RcfConstants
import io.github.tt432.eyelib.Eyelib
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.capability.component.ModelComponent

object PlayerAnimationSetup {

	private val modelInfo = ModelComponent.SerializableInfo(

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
