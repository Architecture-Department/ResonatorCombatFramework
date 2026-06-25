package architecture.resonator_combat_framework.module.collision.event

import architecture.resonator_combat_framework.module.collision.CollisionEntityData
import architecture.resonator_combat_framework.module.collision.CollisionEntry
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

/**
 * 碰撞实体事件 —— 碰撞系统与实体交互的事件基类。
 */
sealed class CollisionEntityEvent(
	val attacker: Entity,
	val colliderId: CollisionEntry,
	val target: Entity,
	var data: CollisionEntityData
) : Event() {

	/**
	 * 碰撞检查事件 —— 在碰撞检测前触发。
	 * 可取消：取消后跳过该目标实体。
	 */
	class Check(
		attacker: Entity,
		colliderId: CollisionEntry,
		target: Entity,
		data: CollisionEntityData
	) : CollisionEntityEvent(attacker, colliderId, target, data), ICancellableEvent

	var isRecord: Boolean = true

	/**
	 * 碰撞命中事件 —— 碰撞检测通过后触发。
	 */
	class Hit(
		attacker: Entity,
		colliderId: CollisionEntry,
		target: Entity,
		data: CollisionEntityData,
	) : CollisionEntityEvent(attacker, colliderId, target, data)
}
