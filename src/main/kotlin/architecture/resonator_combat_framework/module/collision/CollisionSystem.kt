package architecture.resonator_combat_framework.module.collision

import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.collision.event.CollisionHitEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.neoforged.neoforge.common.NeoForge

object CollisionSystem {
	private val reusableEntityBuffer = mutableListOf<Entity>()

	fun getData(entity: Entity): CollisionEntityData {
		return entity.getData(RcfAttachmentTypes.ENTITY_COLLISION)
	}

	fun tick(entity: Entity) {
		if (entity.level().isClientSide) return
		val data = entity.getData(RcfAttachmentTypes.ENTITY_COLLISION) ?: return
		if (data.activeColliders.isEmpty()) return
		data.pruneExpired(entity.level().gameTime)
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
		reusableEntityBuffer.clear()
		level.getEntities(attacker, searchBox) { e ->
			e is LivingEntity && e != attacker && e.isAlive
		}.also { reusableEntityBuffer.addAll(it) }

		for (target in reusableEntityBuffer) {
			for (entry in data.activeColliders) {
				if (data.isAlreadyHit(entry.id, target.id)) continue
				if (!checkCollision(entry, attacker, target)) continue
				data.markHit(entry.id, target.id)
				NeoForge.EVENT_BUS.post(CollisionHitEvent(attacker, entry.id, target))
			}
		}
	}

	private fun checkCollision(entry: CollisionEntry, attacker: Entity, target: Entity): Boolean {
		val shape = entry.shape
		val targetBox = target.boundingBox

		return shape.checkCollision(entry, attacker, targetBox)
	}

}
