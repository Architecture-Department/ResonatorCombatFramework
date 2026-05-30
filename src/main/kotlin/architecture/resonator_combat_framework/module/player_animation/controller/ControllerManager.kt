package architecture.resonator_combat_framework.module.player_animation.controller

import net.minecraft.resources.ResourceLocation

/**
 * 控制器管理器。
 *
 * 双集合存储：Map 提供 O(1) 名称查找，List 保持插入顺序。
 * 先放入的视为优先级更高（排在前面）。
 * isOverriding 的处理范围为比这个控制器先放入的（也就是排在它前面的）。
 */
class ControllerManager {
	/** 名称 → 控制器（O(1) 查找） */
	private val nameMap = mutableMapOf<ResourceLocation, IAnimationController>()

	/** 保持插入顺序的控制器列表 */
	private val ordered = mutableListOf<IAnimationController>()

	/** 追加控制器到末尾 */
	fun add(name: ResourceLocation, controller: IAnimationController) {
		nameMap[name] = controller
		ordered.add(controller)
	}

	/** 在指定索引位置插入控制器 */
	fun add(index: Int, name: ResourceLocation, controller: IAnimationController) {
		nameMap[name] = controller
		ordered.add(index, controller)
	}

	/** 在指定控制器之后插入 */
	fun addAfter(afterName: ResourceLocation, name: ResourceLocation, controller: IAnimationController) {
		val after = nameMap[afterName]
		if (after != null) {
			val idx = ordered.indexOf(after)
			nameMap[name] = controller
			ordered.add(idx + 1, controller)
		} else {
			add(name, controller)
		}
	}

	/** 在指定控制器之前插入 */
	fun addBefore(beforeName: ResourceLocation, name: ResourceLocation, controller: IAnimationController) {
		val before = nameMap[beforeName]
		if (before != null) {
			val idx = ordered.indexOf(before)
			nameMap[name] = controller
			ordered.add(idx, controller)
		} else {
			add(name, controller)
		}
	}

	/** 移除控制器并立即停止 */
	fun remove(name: ResourceLocation) {
		val ctrl = nameMap.remove(name) ?: return
		ctrl.stopImmediate()
		ordered.remove(ctrl)
	}

	/** 按名称获取控制器（O(1)） */
	fun get(name: ResourceLocation): IAnimationController? = nameMap[name]

	/** 获取默认控制器（第一个添加的） */
	fun getDefault(): IAnimationController? = ordered.firstOrNull()

	/** 获取所有控制器（按添加顺序） */
	fun getAll(): List<IAnimationController> = ordered

	/** 是否存在指定控制器（O(1)） */
	fun has(name: ResourceLocation): Boolean = name in nameMap

	/** 任意控制器是否活跃 */
	fun isAnyActive(): Boolean = ordered.any { it.isActive() }

	/** 指定控制器是否活跃（O(1)） */
	fun isActive(name: ResourceLocation): Boolean = nameMap[name]?.isActive() == true

	/** 获取所有活跃控制器（按添加顺序） */
	fun getSortedActive(): List<IAnimationController> =
		ordered.filter { it.isActive() }

	/**
	 * 获取可渲染的控制器列表。
	 *
	 * 从最高优先级到最低优先级遍历活跃控制器。
	 * 如果某个控制器的所有骨骼已被更高优先级控制器渲染，
	 * 且自身没有 isOverriding 标志，则跳过该控制器。
	 * isOverriding 允许低优先级控制器覆盖高优先级控制器的活跃骨骼。
	 */
	fun getRenderable(): List<IAnimationController> {
		val active = getSortedActive()
		val result = mutableListOf<IAnimationController>()
		val renderedBones = mutableSetOf<String>()
		for (ctrl in active) {
			if (ctrl.affectedBones.isNotEmpty() && ctrl.affectedBones.all { it in renderedBones } && !ctrl.isOverriding) {
				continue
			}
			result.add(ctrl)
			renderedBones.addAll(ctrl.affectedBones)
		}
		return result
	}

	/**
	 * 查找阻塞指定控制器的更高优先级控制器。
	 * 返回排在它前面、且 isOverriding 且骨骼有交集的控制器的列表。
	 */
	fun findBlocking(controller: IAnimationController): List<IAnimationController> {
		if (!controller.isActive()) return emptyList()
		val active = getSortedActive()
		val myIndex = active.indexOf(controller)
		if (myIndex < 0) return emptyList()
		val blocking = mutableListOf<IAnimationController>()
		for (i in 0 until myIndex) {
			val higher = active[i]
			if (higher.isOverriding && hasBoneConflict(higher, controller)) blocking.add(higher)
		}
		return blocking
	}

	private fun hasBoneConflict(a: IAnimationController, b: IAnimationController): Boolean {
		val aBones = a.affectedBones
		val bBones = b.affectedBones
		if (aBones.isEmpty() || bBones.isEmpty()) return false
		return aBones.intersect(bBones).isNotEmpty()
	}
}

