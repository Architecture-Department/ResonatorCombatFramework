package architecture.resonator_combat_framework.module.collision

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import java.util.*

/**
 * 实体碰撞数据（Attachment），附着于实体上用于管理其所有碰撞体及命中记录。
 *
 * 碰撞体按 [CollisionEntry.groupId] 分组存储（LinkedHashMap 保持插入顺序），
 * 命中记录按 groupId → 实体 UUID 集合存储。
 *
 * 碰撞体不自动过期，由外部（[AttackAnimation.tickAdvance] 每 tick 刷新、
 * [AttackAnimation.onEnd] 动画结束时清理）管理生命周期。
 *
 * @property holder 持有该碰撞数据的实体
 */
class CollisionEntityData(val holder: Entity) {
	private val colliderMap = LinkedHashMap<ResourceLocation, MutableList<CollisionEntry>>()
	private val hitRecords = mutableMapOf<ResourceLocation, MutableSet<UUID>>()

	private var flatView: List<CollisionEntry> = emptyList()
	private var dirty = false

	/** 所有碰撞体的展平视图（惰性重建，dirty 为 true 时重新展平） */
	val activeColliders: List<CollisionEntry>
		get() {
			if (dirty) {
				flatView = colliderMap.values.flatten()
				dirty = false
			}
			return flatView
		}

	/**
	 * 添加一个碰撞条目到指定分组。
	 *
	 * @param entry 需添加的碰撞条目
	 */
	fun addCollider(entry: CollisionEntry) {
		colliderMap.getOrPut(entry.groupId) { mutableListOf() }.add(entry)
		dirty = true
	}

	/** 清空所有分组的碰撞条目（保留命中记录） */
	fun clearColliders() {
		colliderMap.clear()
		dirty = true
	}

	/**
	 * 移除指定分组 ID 的所有碰撞条目（保留命中记录）。
	 *
	 * @param id 目标分组 ID
	 */
	fun removeColliders(id: ResourceLocation) {
		colliderMap.remove(id)
		dirty = true
	}

	/**
	 * 获取指定分组的碰撞体列表，用于原地更新 worldMatrix 等属性。
	 *
	 * @param groupId 目标分组 ID
	 * @return 碰撞体列表，若分组不存在则返回 null
	 */
	fun getColliders(groupId: ResourceLocation): MutableList<CollisionEntry>? = colliderMap[groupId]


	/**
	 * 移除指定分组及其命中记录（动画结束时调用）。
	 *
	 * @param groupId 目标分组 ID
	 */
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

	/**
	 * 检查指定分组是否已命中过指定实体。
	 *
	 * @param groupId   碰撞分组 ID
	 * @param victimUUID 目标实体 UUID
	 * @return 若该分组已记录对此实体的命中则返回 true
	 */
	fun isAlreadyHit(groupId: ResourceLocation, victimUUID: UUID): Boolean {
		return hitRecords[groupId]?.contains(victimUUID) == true
	}

	/**
	 * 标记指定分组已命中指定实体，后续 tick 不再重复触发。
	 *
	 * @param groupId   碰撞分组 ID
	 * @param victimUUID 目标实体 UUID
	 */
	fun markHit(groupId: ResourceLocation, victimUUID: UUID) {
		hitRecords.getOrPut(groupId) { mutableSetOf() }.add(victimUUID)
	}

	/**
	 * 清空指定分组的所有命中记录。
	 *
	 * @param groupId 目标分组 ID
	 */
	fun clearHitRecords(groupId: ResourceLocation) {
		hitRecords.remove(groupId)
	}

	/** 清空所有分组的命中记录 */
	fun clearAllHitRecords() {
		hitRecords.clear()
	}
}
