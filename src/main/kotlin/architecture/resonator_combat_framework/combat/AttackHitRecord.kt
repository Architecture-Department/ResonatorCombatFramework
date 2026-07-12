package architecture.resonator_combat_framework.combat

import java.util.*

/**
 * 攻击命中记录 —— 按实体隔离，按阶段分组。
 *
 * 每个实体通过 Attachment 持有自己的实例，替代原本 [AttackAnimationAction] 中的静态 [HitRecord]。
 * 每阶段（phaseIdx）独立记录 tried/hit 的实体 UUID，阶段结束时调用 [removeGroup] 清理。
 */
class AttackHitRecord {

	private val tried = mutableMapOf<Int, MutableSet<UUID>>()
	private val hit = mutableMapOf<Int, MutableSet<UUID>>()

	fun getTried(phaseIdx: Int): MutableSet<UUID> = tried.getOrPut(phaseIdx) { mutableSetOf() }
	fun getHit(phaseIdx: Int): MutableSet<UUID> = hit.getOrPut(phaseIdx) { mutableSetOf() }

	/** 获取该阶段的所有已尝试 entity，不存在则返回空集 */
	fun getTriedOrEmpty(phaseIdx: Int): Set<UUID> = tried[phaseIdx] ?: emptySet()
	fun getHitOrEmpty(phaseIdx: Int): Set<UUID> = hit[phaseIdx] ?: emptySet()

	/** 返回当前有记录的 phase 索引集合 */
	val activePhases: Set<Int> get() = tried.keys

	/** 移除指定阶段的记录（阶段结束时调用） */
	fun removeGroup(phaseIdx: Int) {
		tried.remove(phaseIdx)
		hit.remove(phaseIdx)
	}

	/** 清空所有记录（动作结束时调用） */
	fun clear() {
		tried.clear()
		hit.clear()
	}
}
