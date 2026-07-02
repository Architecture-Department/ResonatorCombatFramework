package architecture.resonator_combat_framework.events.collision

import architecture.resonator_combat_framework.combat.AttackAnimationAction
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.collision.event.CollisionEntityEvent
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

/**
 * 碰撞桥接 —— 连接动画模块的碰撞数据与碰撞模块的检测系统。
 *
 * 职责：
 * 1. 监听 [CollisionEntityEvent.Hit] → 查找 [EntityStateHolder] 中的 [AttackAnimationAction] →
 *    调用 [AttackAnimationAction.onHurtEntity] 触发伤害回调。
 */
@EventBusSubscriber
object CollisionBridge {
	@SubscribeEvent
	fun onCollisionHit(event: CollisionEntityEvent.Hit) {
		val attacker = event.attacker
		if (attacker !is LivingEntity) return
		val target = event.target
		if (target !is LivingEntity) return

		val stateHolder = attacker.getExistingData(RcfAttachmentTypes.STATE_HOLDER).orElse(null) ?: return
		val actionController = stateHolder.actionController
		val action = actionController.action as? AttackAnimationAction ?: return
		if (action.id != event.colliderId) return

		val animTime = actionController.time
		val activePhases = action.phases.filter { animTime >= it.startTime && animTime < it.endTime }
		for (phase in activePhases) {
			action.onHurtEntity(attacker, target, phase)
		}
	}
}
