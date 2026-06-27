package architecture.resonator_combat_framework.module.collision

import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.collision.collision.OBB
import architecture.resonator_combat_framework.module.collision.collision.WorldBounds
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import kotlin.math.sqrt

object CollisionSystem {

	@JvmStatic
	fun getData(entity: Entity): CollisionEntityData {
		return entity.getData(RcfAttachmentTypes.ENTITY_COLLISION)
	}

	@JvmStatic
	fun tick(entity: Entity) {
		if (entity.level().isClientSide) return
		val data = entity.getData(RcfAttachmentTypes.ENTITY_COLLISION) ?: return
		if (data.activeColliders.isEmpty()) return
		processEntity(entity, data)
	}

	// ===== 主流程 =====

	private fun processEntity(attacker: Entity, data: CollisionEntityData) {
		val (searchBox, boundsCache) = buildSearchBox(attacker, data) ?: return

		val targets = attacker.level().getEntities(attacker, searchBox) { e ->
			e is LivingEntity && e != attacker && e.isAlive
		}

		for (target in targets) {
			for (entry in data.activeColliders) {
				processCollision(attacker, target, entry, data, boundsCache)
			}
		}
	}

	// ===== 搜索 AABB 构建 =====

	private data class SearchResult(
		val searchBox: AABB,
		val boundsCache: Map<CollisionEntry, WorldBounds>,
	)

	private fun buildSearchBox(attacker: Entity, data: CollisionEntityData): SearchResult? {
		var minX = Double.MAX_VALUE
		var minY = Double.MAX_VALUE
		var minZ = Double.MAX_VALUE
		var maxX = Double.MIN_VALUE
		var maxY = Double.MIN_VALUE
		var maxZ = Double.MIN_VALUE
		var hasCollider = false
		val boundsCache = mutableMapOf<CollisionEntry, WorldBounds>()

		for (entry in data.activeColliders) {
			val bounds = computeWorldBounds(entry, attacker)
			boundsCache[entry] = bounds

			val expand = bounds.sphereRadius.toDouble() + 1.0
			if (bounds.cx - expand < minX) minX = bounds.cx - expand
			if (bounds.cy - expand < minY) minY = bounds.cy - expand
			if (bounds.cz - expand < minZ) minZ = bounds.cz - expand
			if (bounds.cx + expand > maxX) maxX = bounds.cx + expand
			if (bounds.cy + expand > maxY) maxY = bounds.cy + expand
			if (bounds.cz + expand > maxZ) maxZ = bounds.cz + expand
			hasCollider = true
		}

		if (!hasCollider) return null
		return SearchResult(AABB(minX, minY, minZ, maxX, maxY, maxZ), boundsCache)
	}

	// ===== 碰撞处理 =====

	private fun processCollision(
		attacker: Entity,
		target: Entity,
		entry: CollisionEntry,
		data: CollisionEntityData,
		boundsCache: Map<CollisionEntry, WorldBounds>,
	) {
		val check = RcfEventHooks.CollisionEntityCheck(attacker, entry, target, data)
		if (check.isCanceled) return
		if (data.isAlreadyHit(entry.groupId, target.uuid)) return

		if (!isWithinSphereRange(entry, target, boundsCache)) return
		if (!checkCollision(entry, attacker, target)) return
		if (!isValidHit(entry, attacker, target)) return
		if (!entry.hasEffect) return

		if (check.isRecord) {
			data.markHit(entry.groupId, target.uuid)
		}
		RcfEventHooks.CollisionEntityHit(attacker, entry, target, data)
	}

	// ===== 球体距离提前退出 =====

	private fun isWithinSphereRange(
		entry: CollisionEntry,
		target: Entity,
		boundsCache: Map<CollisionEntry, WorldBounds>,
	): Boolean {
		val wb = boundsCache[entry] ?: return false
		val box = target.boundingBox
		val cx = (box.minX + box.maxX) * 0.5
		val cy = (box.minY + box.maxY) * 0.5
		val cz = (box.minZ + box.maxZ) * 0.5
		val distSqr =
			(wb.cx - cx) * (wb.cx - cx) +
				(wb.cy - cy) * (wb.cy - cy) +
				(wb.cz - cz) * (wb.cz - cz)
		val halfExtents = sqrt(
			((box.maxX - box.minX) * 0.5).let { it * it } +
				((box.maxY - box.minY) * 0.5).let { it * it } +
				((box.maxZ - box.minZ) * 0.5).let { it * it }
		).toFloat()
		val maxDist = wb.sphereRadius + halfExtents
		return distSqr <= maxDist * maxDist
	}

	// ===== SAT 碰撞检测 =====

	private fun checkCollision(entry: CollisionEntry, attacker: Entity, target: Entity): Boolean {
		return entry.shape.checkCollision(entry, attacker, target.boundingBox)
	}

	// ===== 世界包围球 =====

	private fun computeWorldBounds(entry: CollisionEntry, attacker: Entity): WorldBounds {
		return entry.shape.computeWorldBounds(entry, attacker)
	}

	// ===== 射线遮挡检查 =====

	private fun isValidHit(entry: CollisionEntry, attacker: Entity, target: Entity): Boolean {
		val mode = entry.raycastMode
		if (mode == CollisionRaycastMode.NONE) return true

		val from = getRaycastOrigin(entry, attacker, mode)
		val to = target.position().add(0.0, target.bbHeight * 0.5, 0.0)
		val clip = attacker.level().clip(
			ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker)
		)
		return clip.type == HitResult.Type.MISS ||
			clip.location.distanceToSqr(from) >= from.distanceToSqr(to) * 0.9
	}

	private fun getRaycastOrigin(entry: CollisionEntry, attacker: Entity, mode: CollisionRaycastMode): Vec3 {
		return when (mode) {
			CollisionRaycastMode.FROM_COLLIDER -> getColliderCenter(entry, attacker)
			CollisionRaycastMode.FROM_ENTITY ->
				attacker.position().add(0.0, attacker.eyeHeight * 0.7, 0.0)

			CollisionRaycastMode.NONE -> Vec3.ZERO
		}
	}

	private fun getColliderCenter(entry: CollisionEntry, attacker: Entity): Vec3 {
		return when (val shape = entry.shape) {
			is OBB -> {
				val worldCenter = if (entry.worldMatrix != null) {
					Vector3f(shape.center).mulPosition(entry.worldMatrix)
				} else {
					Vector3f(
						(attacker.x + shape.center.x).toFloat(),
						(attacker.y + shape.center.y).toFloat(),
						(attacker.z + shape.center.z).toFloat(),
					)
				}
				Vec3(worldCenter.x.toDouble(), worldCenter.y.toDouble(), worldCenter.z.toDouble())
			}

			else -> {
				val bounds = shape.computeWorldBounds(entry, attacker)
				Vec3(bounds.cx, bounds.cy, bounds.cz)
			}
		}
	}
}
