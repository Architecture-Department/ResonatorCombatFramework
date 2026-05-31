// 动画映射器接口。定义实体模型动画映射的生命周期，将玩家模型的特定部分映射到动画骨骼
package architecture.resonator_combat_framework.module.player_animation.api

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.events.registry.AnimationControllerRegistry
import architecture.resonator_combat_framework.module.player_animation.controller.BaseAnimationController
import architecture.resonator_combat_framework.module.player_animation.controller.ControllerManager
import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController
import architecture.resonator_combat_framework.module.player_animation.flags.AnimationPlayData
import architecture.resonator_combat_framework.module.player_animation.flags.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.player_animation.registry.ProxyBoneConfigRegistry
import net.minecraft.resources.ResourceLocation

/**
 * 动画映射器根接口。
 *
 * 管理多个 [IAnimationController]，处理动画 ID 到控制器的路由，
 * 提供统一的触发/停止/暂停/恢复入口。
 */
@AllOpe
interface IAnimationMapper {
	// ==================== 核心属性 ====================
	val isClient: Boolean

	val configLoader: ProxyBoneConfigRegistry
		get() = ProxyBoneConfigRegistry.getInstance(isClient)

	val controllerManager: ControllerManager

	/** 主控制器 */
	val mainController: IAnimationController
		get() = controllerManager.get(AnimationControllerRegistry.MAIN)
			?: controllerManager.getDefault()
			?: error("Default controller not initialized")

	// ==================== 动画触发方法 ====================
	/**
	 * 完整参数模式：控制器 + 动画 + 速度 + 淡入 + 淡出。
	 * 速度在前（Float），淡入/淡出在后（Int），与 transitionTicks 模式通过类型区分。
	 */
	fun trigger(
		controllerName: ResourceLocation = AnimationControllerRegistry.MAIN,
		animId: String,
		speedMultiplier: Float = 1f,
		fadeInTicks: Int = -1,
		fadeOutTicks: Int = -1
	) = trigger(
		AnimationPlayData(
			animId = animId,
			controllerName = controllerName,
			fadeInTicks = fadeInTicks,
			fadeOutTicks = fadeOutTicks,
			speedMultiplier = speedMultiplier
		)
	)

	/**
	 * 简化过渡模式：控制器 + 动画 + 过渡时间 + 速度。
	 * transitionTicks（Int）在速度（Float）前，与 fadeIn/fadeOut 模式通过类型区分。
	 */
	fun trigger(
		controllerName: ResourceLocation = AnimationControllerRegistry.MAIN,
		animId: String,
		transitionTicks: Int = -1,
		speedMultiplier: Float = 1f
	) = trigger(
		AnimationPlayData(
			animId = animId,
			controllerName = controllerName,
			fadeInTicks = transitionTicks,
			fadeOutTicks = transitionTicks,
			speedMultiplier = speedMultiplier
		)
	)

	/** 核心触发动画方法 */
	fun trigger(playData: AnimationPlayData) {
		val controller = controllerManager.get(playData.controllerName) ?: mainController
		val loaded = configLoader.getConfig(playData.animId)
		val used = playData.boneConfig ?: loaded
		val bac = controller as BaseAnimationController
		bac.resolvedBoneConfig = used
		bac.boneConfigs = used
		controller.trigger(playData)
	}

	// ==================== 动画停止方法 ====================
	fun stop(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN, fadeOutTicks: Int = -1) {
		(controllerManager.get(controllerName) ?: mainController).stop(fadeOutTicks)
	}

	fun stopAll(fadeOutTicks: Int = -1) = controllerManager.getAll().forEach { it.stop(fadeOutTicks) }

	fun stopAnimation(animId: String, controllerName: ResourceLocation = AnimationControllerRegistry.MAIN) {
		(controllerManager.get(controllerName) ?: mainController).stop()
	}

	// ==================== 动画暂停/恢复方法 ====================
	/** 暂停所有控制器的动画 */
	fun pauseAll() = controllers().forEach { it.pause() }

	/** 恢复所有控制器的动画 */
	fun resumeAll() = controllers().forEach { it.resume() }

	fun pause(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN) {
		(controllerManager.get(controllerName) ?: mainController).pause()
	}

	fun resume(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN) {
		(controllerManager.get(controllerName) ?: mainController).resume()
	}

	// ==================== 状态查询方法 ====================
	fun isActive(): Boolean = controllerManager.isAnyActive()
	fun isControllerActive(): Boolean = mainController.isActive()
	fun isControllerActive(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN): Boolean =
		(controllerManager.get(controllerName) ?: mainController).isActive()

	// ==================== 控制器获取方法 ====================
	fun getController(): IAnimationController = mainController
	fun getController(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN): IAnimationController =
		controllerManager.get(controllerName) ?: mainController

	fun hasController(): Boolean = controllerManager.getDefault() != null
	fun controllers() = controllerManager.getAll()
	fun hasController(controllerName: ResourceLocation = AnimationControllerRegistry.MAIN): Boolean =
		controllerManager.has(controllerName)

	// ==================== 控制器管理方法 ====================
	fun addController(name: ResourceLocation, controller: IAnimationController) {
		if (name == AnimationControllerRegistry.MAIN) return
		controllerManager.add(name, controller)
	}

	fun removeController(name: ResourceLocation) {
		if (name == AnimationControllerRegistry.MAIN) return
		controllerManager.remove(name)
	}

	// ==================== 高级查询方法 ====================
	fun getActiveControllersSorted(): List<IAnimationController> =
		controllerManager.getSortedActive()

	fun findBlockingControllers(controller: IAnimationController): List<IAnimationController> =
		controllerManager.findBlocking(controller)

	fun getRenderableControllers(): List<IAnimationController> =
		controllerManager.getRenderable()

	/**
	 * 检查两个控制器是否存在骨骼冲突。
	 * 当两者都有 affectedBones 且交集非空时返回 true。
	 */
	fun hasBoneConflict(ctrlA: IAnimationController, ctrlB: IAnimationController): Boolean {
		val a = ctrlA.affectedBones
		val b = ctrlB.affectedBones
		if (a.isEmpty() || b.isEmpty()) return false
		return a.intersect(b).isNotEmpty()
	}

	fun resolveConfig(animId: String): ProxyBoneConfigData = configLoader.getConfig(animId)

	fun tick() {
		controllerManager.getAll().forEach {
			it.tick(this)
		}
	}

	fun tick(gameTime: Float, deltaSec: Float) {
		for (ctrl in controllerManager.getAll()) ctrl.tick(gameTime, deltaSec)
	}

	fun collectProxyModels(): List<ProxyModel> =
		controllerManager.getRenderable().map { (it as BaseAnimationController).proxyModel }

	fun mergedWeight(): Float =
		controllerManager.getRenderable().firstOrNull()?.effectiveWeight ?: mainController.effectiveWeight
}
