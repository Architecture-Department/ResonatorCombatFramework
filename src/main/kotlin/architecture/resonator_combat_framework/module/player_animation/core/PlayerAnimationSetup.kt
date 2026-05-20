package architecture.resonator_combat_framework.module.player_animation.core

import io.github.tt432.eyelib.Eyelib
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.capability.component.ModelComponent
import io.github.tt432.eyelib.molang.MolangValue
import net.minecraft.resources.ResourceLocation

object PlayerAnimationSetup {

	private val modelInfo = ModelComponent.SerializableInfo(
		"resonator_combat_framework:player_proxy",
		ResourceLocation.fromNamespaceAndPath("resonator_combat_framework", "geo/empty"),
		ResourceLocation.fromNamespaceAndPath("eyelib", "entity_cutout_no_cull")
	)

	@Volatile
	private var cachedAnimEntries: Map<String, String>? = null

	@Volatile
	private var cachedAnimMultipliers: Map<String, MolangValue>? = null

	private val animEntries: Map<String, String>
		get() = cachedAnimEntries ?: buildAnimEntries().also { cachedAnimEntries = it }

	private val animMultipliers: Map<String, MolangValue>
		get() = cachedAnimMultipliers ?: buildAnimMultipliers().also { cachedAnimMultipliers = it }

	private fun buildAnimEntries(): Map<String, String> =
		Eyelib.getAnimationManager().allData.keys.associateWith { it }

	private fun buildAnimMultipliers(): Map<String, MolangValue> =
		Eyelib.getAnimationManager().allData.keys.associateWith { MolangValue.ONE }

	fun refresh() {
		cachedAnimEntries = null
		cachedAnimMultipliers = null
	}

	fun setupRenderData(renderData: RenderData<*>) {
		renderData.isUseBuiltInRenderSystem = false

		val mc = ModelComponent()
		mc.setInfo(modelInfo)
		renderData.modelComponents.clear()
		renderData.modelComponents.add(mc)

		renderData.animationComponent.setup(animEntries, animMultipliers)
	}
}
