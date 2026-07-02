package architecture.resonator_combat_framework.events.collision

import architecture.resonator_combat_framework.animation.AttackAnimationDef
import architecture.resonator_combat_framework.module.collision.event.CollisionEntityEvent
import architecture.resonator_combat_framework.module.entity_animation.IAnimationProvider
import architecture.resonator_combat_framework.module.entity_animation.IAnimationProvider.Companion.getMapperProvider
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

/**
 * 碰撞桥接 —— 连接动画模块的碰撞数据与碰撞模块的检测系统。
 *
 * 职责：
 * 1. 监听 [CollisionEntityEvent.Hit] → 查找对应的攻击动画阶段 →
 *    调用 [AttackAnimationDef.onHurtEntity] 触发伤害回调。
 *
 * 此对象不包含任何伤害/击退/眩晕逻辑，仅做数据流转。
 */
@EventBusSubscriber
object CollisionBridge {
	@SubscribeEvent
	fun onCollisionHit(event: CollisionEntityEvent.Hit) {
		val attacker = event.attacker
		if (attacker !is LivingEntity) return
		val target = event.target
		if (target !is LivingEntity) return

		if (attacker !is IAnimationProvider) return

		val manager = attacker.getMapperProvider().animationControllerManager

		for (ctrl in manager.getAll()) {
			val anim = ctrl.currentAnim as? AttackAnimationDef ?: continue
			if (anim.id != event.colliderId) continue

			val animTime = ctrl.currentAnimTime
			val activePhases = anim.getActivePhases(animTime)
			for (phase in activePhases) {
				anim.onHurtEntity(attacker, target, phase)
			}
		}
	}
}
