package architecture.resonator_combat_framework.module.entity_animation.animation.mapper

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.animation.data.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

/** 动画映射器接口，整合触发/控制/查询 */
@AllOpe
interface IEntityAnimationMapperProvider<T : Entity, M : EntityModel<T>> {
	val holder: T
	val isClient: Boolean
	val animationControllerManager: AnimationControllerManager<T>

	/** 实体级 MoLang 数据 */
	val molangData: MolangData get() = MolangData.of(holder)
	val mainController: IEntityAnimationController<T> get() = animationControllerManager.getMainController()

	fun tickAnimationManager()

	fun resolveConfig(animId: String): ProxyBoneConfigData

	/** 客户端 */
	fun tickAndRender(model: M, partialTick: Float, poseStack: PoseStack)

	/** 是否有控制器激活 */
	fun isActive(): Boolean

	/** 获取指定控制器 */
	fun getController(controllerName: ResourceLocation): IEntityAnimationController<T>?

	/** 获取所有控制器 */
	fun controllers(): List<IEntityAnimationController<*>>

	/** 是否有指定控制器 */
	fun hasController(controllerName: ResourceLocation): Boolean

	/** 获取所有激活的控制器 */
	fun getActiveControllersSorted(): List<IEntityAnimationController<T>>

	/** 获取所有可渲染的控制器 */
	fun getRenderableControllers(): List<IEntityAnimationController<T>>

	/** 获取所有阻塞的控制器 */
	fun findBlockingControllers(controller: IEntityAnimationController<T>): List<IEntityAnimationController<T>>

	/** 停止指定控制器 */
	fun stop(controllerName: ResourceLocation, fadeOutTicks: Int = -1)

	fun stop(fadeOutTicks: Int = -1){
		stop(AnimationControllers.MAIN, fadeOutTicks)
	}

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

	/** 触发指定动画 */

	/** 检查指定控制器是否有某种状态被 ActionAnimation 锁定 */
	fun isStateLocked(state: ResourceLocation, controllerName: ResourceLocation = AnimationControllers.MAIN): Boolean
		= (getController(controllerName)?.activeStateModifiers?.get(state) == false)
	fun trigger(controllerName: ResourceLocation, animId: String, playData: AnimationPlayData)

	fun trigger(animId: String, playData: AnimationPlayData) {
		trigger(AnimationControllers.MAIN, animId, playData)
	}
}
