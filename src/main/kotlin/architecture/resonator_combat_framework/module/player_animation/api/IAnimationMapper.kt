package architecture.resonator_combat_framework.module.player_animation.api

import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController

/** 根接口：生命周期 + 控制器管理 + 动画映射 */
interface IAnimationMapper {
	companion object {
		const val DEFAULT_CONTROLLER_NAME: String = "default"
	}

	/** 动画ID → 控制器名称，未映射走默认 */
	val animControllerMap: MutableMap<String, String>

	/** 解析动画ID对应的控制器 */
	fun resolveController(animId: String): String = animControllerMap[animId] ?: DEFAULT_CONTROLLER_NAME

	/** 详细播放配置 */
	fun trigger(config: AnimationPlayConfig)

	/** 在默认控制器上触发 */
	fun trigger(animId: String)

	/** 在指定控制器上触发 */
	fun trigger(controllerName: String, animId: String)


	fun stop(controllerName: String)

	fun stopImmediate(controllerName: String)


	fun stopAll()

	fun stopAllImmediate()

	fun pause(controllerName: String)

	fun resume(controllerName: String)


	fun stopAnimation(animId: String)


	fun stopAnimation(controllerName: String, animId: String)


	fun isActive(): Boolean


	fun isControllerActive(): Boolean


	fun isControllerActive(controllerName: String): Boolean


	fun getController(): IAnimationController


	fun getController(controllerName: String): IAnimationController


	fun hasController(): Boolean


	fun hasController(controllerName: String): Boolean


	fun addController(name: String, controller: IAnimationController)


	fun removeController(name: String)

	// 优先级 + 骨骼冲突

	/** 活跃控制器按优先级降序 */
	fun getActiveControllersSorted(): List<IAnimationController>

	/** 两控制器是否影响同一骨骼，空集合视为无冲突 */
	fun hasBoneConflict(ctrlA: IAnimationController, ctrlB: IAnimationController): Boolean {
		val bonesA = ctrlA.affectedBones
		val bonesB = ctrlB.affectedBones
		if (bonesA.isEmpty() || bonesB.isEmpty()) return false
		return bonesA.intersect(bonesB).isNotEmpty()
	}

	/** 因骨骼冲突阻塞当前控制器的更高优先级控制器列表 */
	fun findBlockingControllers(controller: IAnimationController): List<IAnimationController>
}
