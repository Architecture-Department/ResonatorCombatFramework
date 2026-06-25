package architecture.resonator_combat_framework.module.collision

import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.collision.collision.OBB
import architecture.resonator_combat_framework.module.collision.event.CollisionEntityEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.NeoForge
import org.joml.Vector3f

object CollisionSystem {
	fun getData(entity: Entity): CollisionEntityData {
		return entity.getData(RcfAttachmentTypes.ENTITY_COLLISION)
	}

	fun tick(entity: Entity) {
		if (entity.level().isClientSide) return
		val data = entity.getData(RcfAttachmentTypes.ENTITY_COLLISION) ?: return
		if (data.activeColliders.isEmpty()) return
		processEntity(entity, data)
	}

	private fun processEntity(attacker: Entity, data: CollisionEntityData) {
		val level = attacker.level()

		var minX = Double.MAX_VALUE
		var minY = Double.MAX_VALUE
		var minZ = Double.MAX_VALUE
		var maxX = Double.MIN_VALUE
		var maxY = Double.MIN_VALUE
		var maxZ = Double.MIN_VALUE
		var hasCollider = false

		for (entry in data.activeColliders) {
			val shape = entry.shape

			val bounds = shape.computeWorldBounds(entry, attacker)

			val expand = bounds.sphereRadius.toDouble() + 2.0
			if (bounds.cx - expand < minX) minX = bounds.cx - expand
			if (bounds.cy - expand < minY) minY = bounds.cy - expand
			if (bounds.cz - expand < minZ) minZ = bounds.cz - expand
			if (bounds.cx + expand > maxX) maxX = bounds.cx + expand
			if (bounds.cy + expand > maxY) maxY = bounds.cy + expand
			if (bounds.cz + expand > maxZ) maxZ = bounds.cz + expand
			hasCollider = true
		}

		if (!hasCollider) return

		val searchBox = AABB(minX, minY, minZ, maxX, maxY, maxZ)
		val reusableEntityBuffer = mutableListOf<Entity>()
		level.getEntities(attacker, searchBox) { e ->
			e is LivingEntity && e != attacker && e.isAlive
		}.also { reusableEntityBuffer.addAll(it) }

		for (target in reusableEntityBuffer) {
			for (entry in data.activeColliders) {
				val check = NeoForge.EVENT_BUS.post(CollisionEntityEvent.Check(attacker, entry, target, data))
				if (check.isCanceled) continue
				if (data.isAlreadyHit(entry.groupId, target.uuid)) continue
				if (!checkCollision(entry, attacker, target)) continue
				if (!isValidHit(entry, attacker, target)) continue
				if (!entry.hasEffect) continue
				if (check.isRecord) {
					data.markHit(entry.groupId, target.uuid)
				}
				NeoForge.EVENT_BUS.post(CollisionEntityEvent.Hit(attacker, entry, target, data))
			}
		}
	}

	private fun checkCollision(entry: CollisionEntry, attacker: Entity, target: Entity): Boolean {
		val shape = entry.shape
		val targetBox = target.boundingBox

		return shape.checkCollision(entry, attacker, targetBox)
	}

	/**
	 * 验证碰撞是否有效（射线遮挡检查）。
	 * 防止碰撞体穿透方块打到目标。
	 */
	private fun isValidHit(entry: CollisionEntry, attacker: Entity, target: Entity): Boolean {
		val mode = entry.raycastMode
		if (mode == CollisionRaycastMode.NONE) return true

		val from = when (mode) {
			CollisionRaycastMode.FROM_COLLIDER -> {
				when (val shape = entry.shape) {
					is OBB -> {
						val obb = shape
						val worldCenter = if (entry.worldMatrix != null) {
							Vector3f(obb.center).mulPosition(entry.worldMatrix)
						} else {
							Vector3f(
								(attacker.x + obb.center.x).toFloat(),
								(attacker.y + obb.center.y).toFloat(),
								(attacker.z + obb.center.z).toFloat(),
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

			CollisionRaycastMode.FROM_ENTITY ->
				attacker.position().add(0.0, attacker.eyeHeight * 0.7, 0.0)
		}
		val to = target.position().add(0.0, target.bbHeight * 0.5, 0.0)
		val clip = attacker.level().clip(
			ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker)
		)
		return clip.type == HitResult.Type.MISS ||
			clip.location.distanceToSqr(from) >= from.distanceToSqr(to) * 0.9
	}
}
