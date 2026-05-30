// 动画映射器接口。定义实体模型动画映射的生命周期，将玩家模型的特定部分映射到动画骨骼
package architecture.resonator_combat_framework.module.player_animation.api

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController

/**
 * 动画映射器根接口。
 *
 * 管理多个 [IAnimationController]，处理动画 ID 到控制器的路由，
 * 提供统一的触发/停止/暂停/恢复入口。
 */
@AllOpe
interface IAnimationMapper {

	// ═══════════════════ 触发 ═══════════════════

	/** 使用完整配置触发动画（包含控制器路由、淡入淡出等） */
	fun trigger(config: AnimationPlayConfig)

	/** 触发动画，使用默认配置 */
	fun trigger(animId: String) = trigger(AnimationPlayConfig.of(animId))

	/**
	 * 在指定控制器上触发动画。
	 * @param controllerName 目标控制器名称
	 * @param animId 动画 ID
	 */
	fun trigger(controllerName: String, animId: String) =
		trigger(AnimationPlayConfig.of(animId).copy(controllerName = controllerName))

	// ═══════════════════ 停止 ═══════════════════

	/** 停止指定控制器的动画（使用配置中的淡出时间） */
	fun stop(controllerName: String)

	/** 立即停止指定控制器的动画，无过渡 */
	fun stopImmediate(controllerName: String)

	/** 停止所有控制器的动画 */
	fun stopAll() = controllers().forEach { it.stop() }

	/** 停止所有控制器的动画，指定淡出时间 */
	fun stopAll(fadeOutTicks: Int) = controllers().forEach { it.stop(fadeOutTicks) }

	/** 立即停止所有控制器的动画，无过渡 */
	fun stopAllImmediate() = controllers().forEach { it.stopImmediate() }

	// ═══════════════════ 暂停 / 恢复 ═══════════════════

	/** 暂停指定控制器的动画 */
	fun pause(controllerName: String)

	/** 恢复指定控制器的动画 */
	fun resume(controllerName: String)

	/** 暂停所有控制器的动画 */
	fun pauseAll() = controllers().forEach { it.pause() }

	/** 恢复所有控制器的动画 */
	fun resumeAll() = controllers().forEach { it.resume() }

	// ═══════════════════ 动画管理 ═══════════════════

	/** 在所有控制器中查找并停止指定动画 ID */
	fun stopAnimation(animId: String)

	/** 在指定控制器中停止指定动画 ID */
	fun stopAnimation(controllerName: String, animId: String)

	// ═══════════════════ 状态查询 ═══════════════════

	/** 任意控制器是否有动画运行 */
	fun isActive(): Boolean

	/** 默认控制器是否有动画运行 */
	fun isControllerActive(): Boolean

	/** 指定控制器是否有动画运行 */
	fun isControllerActive(controllerName: String): Boolean

	/** 获取默认控制器 */
	fun getController(): IAnimationController

	/** 获取指定控制器 */
	fun getController(controllerName: String): IAnimationController

	/** 是否有默认控制器 */
	fun hasController(): Boolean

	/** 是否有指定控制器 */
	fun hasController(controllerName: String): Boolean

	/** 当前活跃的控制器集，按优先级降序排列 */
	fun getActiveControllersSorted(): List<IAnimationController>

	// ═══════════════════ 控制器管理 ═══════════════════

	/** 获取所有控制器 */
	fun controllers(): Collection<IAnimationController>

	/** 添加控制器 */
	fun addController(name: String, controller: IAnimationController)

	/** 移除控制器 */
	fun removeController(name: String)

	// ═══════════════════ 优先级与骨骼冲突 ═══════════════════

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

	/** 查找所有与指定控制器存在骨骼冲突的控制器 */
	fun findBlockingControllers(controller: IAnimationController): List<IAnimationController>
}
