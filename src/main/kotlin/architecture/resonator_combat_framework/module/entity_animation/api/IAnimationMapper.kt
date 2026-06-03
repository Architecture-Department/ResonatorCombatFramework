package architecture.resonator_combat_framework.module.entity_animation.api

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.events.registry.AnimationControllerRegistry
import architecture.resonator_combat_framework.module.entity_animation.controller.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.controller.IAnimationController
import architecture.resonator_combat_framework.module.entity_animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneConfigData
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.resources.ResourceLocation

/** 动画触发接口 */
@AllOpe
interface IAnimationTrigger {
	fun trigger(playData: AnimationPlayData)
}

/** 动画控制接口（停止/暂停/恢复） */
@AllOpe
interface IAnimationControl {
	fun stop(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN, fadeOutTicks: Int = -1)
	fun stopAll(fadeOutTicks: Int = -1)
	fun pause(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN)
	fun resume(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN)
	fun pauseAll()
	fun resumeAll()
}

/** 控制器查询接口 */
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

/** 动画映射器接口，整合触发/控制/查询 */
@AllOpe
interface IAnimationMapper : IAnimationTrigger, IAnimationControl, IControllerQuery {
	val isClient: Boolean

	fun addController(name: ResourceLocation, controller: IAnimationController)
	fun removeController(name: ResourceLocation)
	fun tickAnimations()
	fun resolveConfig(animId: String): ProxyBoneConfigData

	/** 客户端 */
	fun tickAndRender(model: EntityModel<*>, partialTick: Float, poseStack: PoseStack) = Unit
}
