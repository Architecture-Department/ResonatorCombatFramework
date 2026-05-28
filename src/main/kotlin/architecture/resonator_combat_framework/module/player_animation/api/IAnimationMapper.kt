package architecture.resonator_combat_framework.module.player_animation.api

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController

/** 根接口：生命周期 + 控制器管理 + 动画映射 */
@AllOpe
interface IAnimationMapper {
	companion object {
		const val DEFAULT_CONTROLLER_NAME: String = "default"
	}

	/** 动画ID → 控制器名称，未映射走默认 */
	val animControllerMap: MutableMap<String, String>

	fun resolveController(animId: String): String = animControllerMap[animId] ?: DEFAULT_CONTROLLER_NAME

	// 触发

	fun trigger(config: AnimationPlayConfig)

	fun trigger(animId: String) = trigger(AnimationPlayConfig.of(animId))

	fun trigger(controllerName: String, animId: String) =
		trigger(AnimationPlayConfig.of(animId).copy(controllerName = controllerName))

	// 停止

	fun stop(controllerName: String)
	fun stopImmediate(controllerName: String)
	fun stopAll() = controllers().forEach { it.stop() }
	fun stopAll(fadeOutTicks: Int) = controllers().forEach { it.stop(fadeOutTicks) }
	fun stopAllImmediate() = controllers().forEach { it.stopImmediate() }

	// 暂停 / 恢复

	fun pause(controllerName: String)
	fun resume(controllerName: String)
	fun pauseAll() = controllers().forEach { it.pause() }
	fun resumeAll() = controllers().forEach { it.resume() }

	// 动画管理

	fun stopAnimation(animId: String)
	fun stopAnimation(controllerName: String, animId: String)

	// 查询

	fun isActive(): Boolean
	fun isControllerActive(): Boolean
	fun isControllerActive(controllerName: String): Boolean
	fun getController(): IAnimationController
	fun getController(controllerName: String): IAnimationController
	fun hasController(): Boolean
	fun hasController(controllerName: String): Boolean

	// 控制器管理

	fun controllers(): Collection<IAnimationController>
	fun addController(name: String, controller: IAnimationController)
	fun removeController(name: String)

	// 优先级 + 骨骼冲突

	fun getActiveControllersSorted(): List<IAnimationController>

	fun hasBoneConflict(ctrlA: IAnimationController, ctrlB: IAnimationController): Boolean {
		val a = ctrlA.affectedBones
		val b = ctrlB.affectedBones
		if (a.isEmpty() || b.isEmpty()) return false
		return a.intersect(b).isNotEmpty()
	}

	fun findBlockingControllers(controller: IAnimationController): List<IAnimationController>
}
