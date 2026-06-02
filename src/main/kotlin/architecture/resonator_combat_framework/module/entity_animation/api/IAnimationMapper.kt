package architecture.resonator_combat_framework.module.entity_animation.api

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.events.registry.AnimationControllerRegistry
import architecture.resonator_combat_framework.module.entity_animation.controller.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.controller.IAnimationController
import architecture.resonator_combat_framework.module.entity_animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.entity_animation.registry.ProxyBoneConfigRegistry
import net.minecraft.resources.ResourceLocation

@AllOpe
interface IAnimationTrigger {
	fun trigger(playData: AnimationPlayData)
}

@AllOpe
interface IAnimationControl {
	fun stop(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN, fadeOutTicks: Int = -1)
	fun stopAll(fadeOutTicks: Int = -1)
	fun pause(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN)
	fun resume(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN)
	fun pauseAll()
	fun resumeAll()
}

@AllOpe
interface IControllerQuery {
	val animationControllerManager: AnimationControllerManager
	val mainController: IAnimationController
		get() = animationControllerManager.getMainController()

	fun isActive(): Boolean
	fun getController(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN): IAnimationController
	fun controllers(): List<IAnimationController>
	fun hasController(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN): Boolean
	fun getActiveControllersSorted(): List<IAnimationController>
	fun getRenderableControllers(): List<IAnimationController>
	fun findBlockingControllers(controller: IAnimationController): List<IAnimationController>
}

@AllOpe
interface IAnimationMapper : IAnimationTrigger, IAnimationControl, IControllerQuery {
	val configLoader: ProxyBoneConfigRegistry
		get() = ProxyBoneConfigRegistry.getInstance(isClient)
	val isClient: Boolean

	fun addController(name: ResourceLocation, controller: IAnimationController)
	fun removeController(name: ResourceLocation)
	fun tickAnimations()
	fun resolveConfig(animId: String): ProxyBoneConfigData
}

