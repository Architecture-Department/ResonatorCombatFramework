package architecture.resonator_combat_framework.module.player_animation.core

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.client.RcfPlayerAnimationBridge
import io.github.tt432.eyelib.Eyelib
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.capability.component.ModelComponent
import io.github.tt432.eyelib.molang.MolangValue
import net.minecraft.resources.ResourceLocation

object PlayerAnimationSetup {

	private val modelInfo = ModelComponent.SerializableInfo(
		// 注：模型id是模型中的identifier决定的不通过路径或其他
		Rcf.modRlText("player_proxy"),
		Rcf.modRl("empty"),
		ResourceLocation.parse("${Eyelib.MOD_ID}:entity_cutout_no_cull")
	)

	fun setupRenderData(renderData: RenderData<*>) {
		renderData.isUseBuiltInRenderSystem = false

		val modelComponent = ModelComponent()
		modelComponent.setInfo(modelInfo)
		renderData.modelComponents.clear()
		renderData.modelComponents.add(modelComponent)

		renderData.animationComponent.setup(
			mapOf("player" to RcfPlayerAnimationBridge.NAME),
			mapOf("player" to MolangValue.ONE)
		)
	}
}
