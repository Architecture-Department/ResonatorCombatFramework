package architecture.resonator_combat_framework.module.collision

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

/**
 * 实体碰撞数据（Attachment）。
 *
 * 通过 NeoForge Attachment 系统挂载到 [net.minecraft.world.entity.Entity] 上。
 * 外部系统可直接获取修改：
 * ```
 * val data = entity.getData(RcfAttachmentTypes.COLLISION_ENTITY)
 * data.addCollider(entry)
 * ```
 *
 * @property activeColliders 当前活跃的碰撞条目列表。
 * 每 tick 由 [CollisionSystem] 读取处理后，会清除过期条目。
 * @property hitRecords 命中记录。Long 编码：高 32 位 = colliderId.hashCode，低 32 位 = victimId。
 * 用于跨 tick 防重复命中。
 */
class CollisionEntityData(val holder: Entity) {
	/** 当前活跃的碰撞条目 */
	val activeColliders = mutableListOf<CollisionEntry>()

	/** 命中记录：packed long = (colliderIdHash << 32) | victimId */
	private val hitRecords = mutableSetOf<Long>()

	/** 添加一个碰撞条目 */
	fun addCollider(entry: CollisionEntry) {
		activeColliders.add(entry)
	}

	/** 清空所有碰撞条目 */
	fun clearColliders() {
		activeColliders.clear()
	}

	/** 移除指定 ID 的所有碰撞条目 */
	fun removeColliders(id: ResourceLocation) {
		activeColliders.removeAll { it.id == id }
	}

	/** 是否已被指定 collider 命中过指定实体 */
	fun isAlreadyHit(colliderId: ResourceLocation, victimId: Int): Boolean {
		return encode(colliderId, victimId) in hitRecords
	}

	/** 标记指定 collider 已命中指定实体 */
	fun markHit(colliderId: ResourceLocation, victimId: Int) {
		hitRecords.add(encode(colliderId, victimId))
	}

	/** 清空指定 collider 的所有命中记录 */
	fun clearHitRecords(colliderId: ResourceLocation) {
		val hash = colliderId.hashCode().toLong()
		hitRecords.removeAll { (it shr 32) == hash }
	}

	/** 清空所有命中记录 */
	fun clearAllHitRecords() {
		hitRecords.clear()
	}

	/** 清除过期碰撞条目 */
	fun pruneExpired(currentTick: Long) {
		activeColliders.removeAll { it.expiryTick < currentTick }
	}

	companion object {
		private fun encode(id: ResourceLocation, victimId: Int): Long {
			return (id.hashCode().toLong() shl 32) or (victimId.toLong() and 0xFFFFFFFFL)
		}
	}
}
