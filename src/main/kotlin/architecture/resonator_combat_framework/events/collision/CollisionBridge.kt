package architecture.resonator_combat_framework.events.collision

import architecture.resonator_combat_framework.animation.AttackAnimation
import architecture.resonator_combat_framework.module.collision.CollisionEntry
import architecture.resonator_combat_framework.module.collision.CollisionShape
import architecture.resonator_combat_framework.module.collision.CollisionSystem
import architecture.resonator_combat_framework.module.collision.event.CollisionHitEvent
import architecture.resonator_combat_framework.module.collision.event.GatherCollidersEvent
import architecture.resonator_combat_framework.module.entity_animation.mixed.IAnimationProxyProvider
import architecture.resonator_combat_framework.module.entity_animation.mixed.IAnimationProxyProvider.Companion.getAnimationTransformer
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

/**
 * 碰撞桥接 —— 连接动画模块的碰撞数据与碰撞模块的检测系统。
 *
 * 职责：
 * 1. 监听 [architecture.resonator_combat_framework.module.collision.event.GatherCollidersEvent] → 将碰撞体写入 [CollisionEntityData]
 * 2. 监听 [CollisionHitEvent] → 调用 [AttackAnimation.onHurtEntity]
 *
 * 此对象不包含任何伤害/击退/眩晕逻辑，仅做数据流转。
 */
@EventBusSubscriber
object CollisionBridge {
	@SubscribeEvent
	fun onGatherColliders(event: GatherCollidersEvent) {
		val entity = event.entity
		val collisionData = CollisionSystem.getData(entity)

		for (entry in event.getColliders()) {
			collisionData.addCollider(
				CollisionEntry(
					id = entry.id,
					shape = CollisionShape.OBB(
						boneName = entry.boneName,
						center = entry.center,
						halfExtents = entry.halfExtents,
					),
					worldMatrix = entry.worldMatrix,
				)
			)
		}
	}

	@SubscribeEvent
	fun onCollisionHit(event: CollisionHitEvent) {
		val attacker = event.attacker
		if (attacker !is LivingEntity) return
		val target = event.target
		if (target !is LivingEntity) return

		if (attacker !is IAnimationProxyProvider) return

		val manager = attacker.getAnimationTransformer().animationControllerManager

		for (ctrl in manager.getAll()) {
			val anim = ctrl.currentAnim as? AttackAnimation ?: continue
			if (anim.id != event.colliderId) continue

			val animTime = ctrl.currentAnimTime
			val activePhases = anim.getActivePhases(animTime)
			for (phase in activePhases) {
				anim.onHurtEntity(attacker, target, phase)
			}
		}
	}
}
