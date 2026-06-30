package architecture.resonator_combat_framework.module.collision

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import java.util.*

/**
 * 实体碰撞数据（Attachment）。
 *
 * 碰撞体按 [CollisionEntry.groupId] 分组存储（LinkedHashMap），
 * 命中记录按 groupId → 实体 UUID 集合存储。
 *
 * 碰撞体不自动过期，由外部（[AttackAnimation.tickAdvance] 每 tick 刷新、
 * [AttackAnimation.onEnd] 动画结束时清理）管理生命周期。
 */
class CollisionEntityData(val holder: Entity) {
	private val colliderMap = LinkedHashMap<ResourceLocation, MutableList<CollisionEntry>>()
	private val hitRecords = mutableMapOf<ResourceLocation, MutableSet<UUID>>()

	private var flatView: List<CollisionEntry> = emptyList()
	private var dirty = false

	/** 所有碰撞体的展平视图（惰性重建） */
	val activeColliders: List<CollisionEntry>
		get() {
			if (dirty) {
				flatView = colliderMap.values.flatten()
				dirty = false
			}
			return flatView
		}

	/** 添加一个碰撞条目 */
	fun addCollider(entry: CollisionEntry) {
		colliderMap.getOrPut(entry.groupId) { mutableListOf() }.add(entry)
		dirty = true
	}

	/** 清空所有碰撞条目 */
	fun clearColliders() {
		colliderMap.clear()
		dirty = true
	}

	/** 移除指定 ID 的碰撞条目（保留命中记录） */
	fun removeColliders(id: ResourceLocation) {
		colliderMap.remove(id)
		dirty = true
	}

	/** 获取指定分组的碰撞体列表，用于原地更新 worldMatrix 等属性 */
	fun getColliders(groupId: ResourceLocation): MutableList<CollisionEntry>? = colliderMap[groupId]


	/** 移除指定分组及其命中记录（动画结束时调用） */
	fun removeGroup(groupId: ResourceLocation) {
		colliderMap.remove(groupId)
		hitRecords.remove(groupId)
		dirty = true
	}

	/** 清空所有碰撞体和命中记录 */
	fun clearAll() {
		colliderMap.clear()
		hitRecords.clear()
		dirty = true
	}

	/** 是否已被指定分组命中过指定实体 */
	fun isAlreadyHit(groupId: ResourceLocation, victimUUID: UUID): Boolean {
		return hitRecords[groupId]?.contains(victimUUID) == true
	}

	/** 标记指定分组已命中指定实体 */
	fun markHit(groupId: ResourceLocation, victimUUID: UUID) {
		hitRecords.getOrPut(groupId) { mutableSetOf() }.add(victimUUID)
	}

	/** 清空指定分组的所有命中记录 */
	fun clearHitRecords(groupId: ResourceLocation) {
		hitRecords.remove(groupId)
	}

	/** 清空所有命中记录 */
	fun clearAllHitRecords() {
		hitRecords.clear()
	}
}
