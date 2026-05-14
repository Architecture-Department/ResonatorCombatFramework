package architecture.resonator_combat_framework.module.player_animation.util

import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationController

object AnimatableManagerControllerRegistrarUtil {
	@JvmStatic
	fun build(registrar: AnimatableManager.ControllerRegistrar): Map<String, AnimationController<*>> {
		val map: MutableMap<String, AnimationController<*>> = LinkedHashMap()
		registrar.controllers().forEach { map[it.getName()] = it }
		return map
	}
}