package architecture.resonator_combat_framework.module.player_animation.util

import architecture.resonator_combat_framework.module.player_animation.mixin.eye_lib.AnimationManagerAccessor
import architecture.resonator_combat_framework.module.player_animation.mixin.eye_lib.ModelManagerAccessor
import io.github.tt432.eyelib.Eyelib
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.client.animation.Animation
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry
import io.github.tt432.eyelib.client.manager.AnimationManager
import io.github.tt432.eyelib.client.manager.ModelManager
import io.github.tt432.eyelib.molang.MolangValue

object EyeLibUtil {
	private val ANIMATION_MANAGER_INSTANCE = AnimationManagerAccessor.newAnimationManager()
	private val MODEL_MANAGER_INSTANCE = ModelManagerAccessor.newModelManager()

	@JvmStatic
	fun getAnimationManager(isClient: Boolean): AnimationManager {
		return if (isClient) Eyelib.getAnimationManager() else ANIMATION_MANAGER_INSTANCE
	}

	@JvmStatic
	fun getModelManager(isClient: Boolean): ModelManager {
		return if (isClient) Eyelib.getModelManager() else MODEL_MANAGER_INSTANCE
	}

	@JvmStatic
	fun getAnimation(isClient: Boolean, animId: String): Animation<*>? {
		return getAnimationManager(isClient).get(animId)
	}

	@JvmStatic
	fun getEntryData(renderData: RenderData<*>, animId: String): BrAnimationEntry.Data? {
		return renderData.animationComponent.getAnimationData(animId) as? BrAnimationEntry.Data
	}

	@JvmStatic
	fun resetAnimData(renderData: RenderData<*>, animId: String) {
		val data = getEntryData(renderData, animId) ?: return
		data.animTime = 0f
		data.lastTicks = 0f
		data.deltaTime = 0f
	}

	@JvmStatic
	fun getAnimTime(renderData: RenderData<*>, animId: String): Float? {
		return getEntryData(renderData, animId)?.animTime
	}

	@JvmStatic
	fun setAnimTime(renderData: RenderData<*>, animId: String, time: Float) {
		getEntryData(renderData, animId)?.let { it.animTime = time }
	}

	@JvmStatic
	fun setAnimateEntry(renderData: RenderData<*>, anim: Animation<*>, multiplier: MolangValue) {
		renderData.animationComponent.animate[anim] = multiplier
	}

	@JvmStatic
	fun removeAnimateEntries(renderData: RenderData<*>, anims: Collection<Animation<*>>) {
		val animate = renderData.animationComponent.animate
		for (anim in anims) {
			animate.remove(anim)
		}
	}

	@JvmStatic
	fun animateSetup(
		renderData: RenderData<*>,
		animationNames: Map<String, String>,
		multipliers: Map<String, MolangValue>
	) {
		renderData.animationComponent.setup(animationNames, multipliers)
	}
}
