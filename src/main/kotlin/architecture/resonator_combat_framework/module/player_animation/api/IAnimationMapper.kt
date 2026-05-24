package architecture.resonator_combat_framework.module.player_animation.api

import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController

/** 根接口 — 动画生命周期 + 控制器管理 + 动画→控制器映射 */
interface IAnimationMapper {
	companion object {
		const val DEFAULT_CONTROLLER_NAME: String = "default"
	}

	/** 动画ID → 控制器名称 映射, 未映射的动画使用默认控制器 */
	val animControllerMap: MutableMap<String, String>

	/** 根据动画ID解析控制器名称 */
	fun resolveController(animId: String): String = animControllerMap[animId] ?: DEFAULT_CONTROLLER_NAME

	/** 在默认控制器上触发动画, 已存在则重启 */
	fun trigger(animId: String)

	/** 在指定控制器上触发动画 */
	fun trigger(controllerName: String, animId: String)

	/** 停止指定控制器的动画 */
	fun stop(controllerName: String)

	/** 停止所有控制器的动画 */
	fun stopAll()

	/** 从默认控制器移除指定动画 */
	fun stopAnimation(animId: String)

	/** 从指定控制器移除动画 */
	fun stopAnimation(controllerName: String, animId: String)

	/** 任一控制器是否活跃 */
	fun isActive(): Boolean

	/** 默认控制器是否活跃 */
	fun isControllerActive(): Boolean

	/** 指定控制器是否活跃 */
	fun isControllerActive(controllerName: String): Boolean

	/** 获取默认控制器 */
	fun getController(): IAnimationController

	/** 获取指定名称的控制器, 不存在则返回默认 */
	fun getController(controllerName: String): IAnimationController

	/** 默认控制器是否存在 (始终为 true) */
	fun hasController(): Boolean

	/** 指定名称的控制器是否存在 */
	fun hasController(controllerName: String): Boolean

	/** 注册新控制器 */
	fun addController(name: String, controller: IAnimationController)

	/** 移除控制器 (默认控制器不可移除) */
	fun removeController(name: String)

	// ---- 优先级与骨骼冲突检测 ----

	/**
	 * 获取所有活跃的控制器, 按优先级降序排列 (高优先级在前).
	 * 低优先级控制器可能被高优先级覆盖控制器阻塞.
	 */
	fun getActiveControllersSorted(): List<IAnimationController>

	/**
	 * 检查两个控制器的动画是否影响相同的骨骼 (存在骨骼冲突).
	 * 如果任一控制器的 affectedBones 为空集合, 视为无冲突.
	 */
	fun hasBoneConflict(ctrlA: IAnimationController, ctrlB: IAnimationController): Boolean {
		val bonesA = ctrlA.affectedBones
		val bonesB = ctrlB.affectedBones
		if (bonesA.isEmpty() || bonesB.isEmpty()) return false
		return bonesA.intersect(bonesB).isNotEmpty()
	}

	/**
	 * 给定一个控制器, 检查它是否被更高优先级的覆盖控制器阻塞 (因子骨骼冲突).
	 * 返回阻塞它的控制器列表 (按优先级降序).
	 */
	fun findBlockingControllers(controller: IAnimationController): List<IAnimationController>
}
