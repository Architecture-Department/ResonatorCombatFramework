package architecture.resonator_combat_framework.module.entity_animation.api

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.controller.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.controller.IAnimationController
import architecture.resonator_combat_framework.module.entity_animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneConfigData
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.resources.ResourceLocation
import java.util.function.DoubleSupplier

/** 动画触发接口 */
@AllOpe
interface IAnimationTrigger {
	/** 触发指定动画 */
	fun trigger(playData: AnimationPlayData)
}

/** 动画控制接口（停止/暂停/恢复） */
@AllOpe
interface IAnimationControl {
	/** 停止指定控制器 */
	fun stop(controllerName: ResourceLocation = AnimationControllers.MAIN, fadeOutTicks: Int = -1)

	/** 停止所有控制器 */
	fun stopAll(fadeOutTicks: Int = -1)

	/** 暂停指定控制器 */
	fun pause(controllerName: ResourceLocation = AnimationControllers.MAIN)

	/** 恢复所有控制器 */
	fun resume(controllerName: ResourceLocation = AnimationControllers.MAIN)

	/** 暂停所有控制器 */
	fun pauseAll()

	/** 恢复所有控制器 */
	fun resumeAll()
}

/** 控制器查询接口 */
@AllOpe
interface IControllerQuery {
	/** 实体级 MoLang 变量作用域 */
	val molangVariables: MutableMap<String, DoubleSupplier>
		get() = animationControllerManager.molangScope
	val animationControllerManager: AnimationControllerManager
	val mainController: IAnimationController
		get() = animationControllerManager.getMainController()

	/** 是否有控制器激活 */
	fun isActive(): Boolean

	/** 获取指定控制器 */
	fun getController(controllerName: ResourceLocation = AnimationControllers.MAIN): IAnimationController?

	/** 获取所有控制器 */
	fun controllers(): List<IAnimationController>

	/** 是否有指定控制器 */
	fun hasController(controllerName: ResourceLocation = AnimationControllers.MAIN): Boolean

	/** 获取所有激活的控制器 */
	fun getActiveControllersSorted(): List<IAnimationController>

	/** 获取所有可渲染的控制器 */
	fun getRenderableControllers(): List<IAnimationController>

	/** 获取所有阻塞的控制器 */
	fun findBlockingControllers(controller: IAnimationController): List<IAnimationController>
}

/** 动画映射器接口，整合触发/控制/查询 */
@AllOpe
interface IAnimationMapper : IAnimationTrigger, IAnimationControl, IControllerQuery {
	val isClient: Boolean

	fun tickAnimations()
	fun resolveConfig(animId: String): ProxyBoneConfigData

	/** 客户端 */
	fun tickAndRender(model: EntityModel<*>, partialTick: Float, poseStack: PoseStack) = Unit
}
