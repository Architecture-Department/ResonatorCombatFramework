package architecture.resonator_combat_framework.module.player_animation.util

import architecture.resonator_combat_framework.module.player_animation.mixin.eye_lib.AnimationManagerAccessor
import architecture.resonator_combat_framework.module.player_animation.mixin.eye_lib.ModelManagerAccessor
import io.github.tt432.eyelib.Eyelib
import io.github.tt432.eyelib.client.manager.AnimationManager
import io.github.tt432.eyelib.client.manager.ModelManager

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
}