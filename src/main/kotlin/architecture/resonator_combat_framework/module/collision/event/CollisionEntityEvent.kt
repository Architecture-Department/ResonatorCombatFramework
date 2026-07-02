package architecture.resonator_combat_framework.module.collision.event

import architecture.resonator_combat_framework.module.collision.CollisionEntityData
import architecture.resonator_combat_framework.module.collision.CollisionEntry
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

/**
 * 碰撞实体事件 —— 碰撞系统与实体交互的事件基类，所有碰撞相关事件均继承此类。
 *
 * @property attacker   攻击者（发起碰撞的实体）
 * @property colliderId 触发的碰撞条目
 * @property target     被碰撞的目标实体
 * @property data       攻击者的碰撞数据
 */
sealed class CollisionEntityEvent(
	val attacker: Entity,
	val colliderId: CollisionEntry,
	val target: Entity,
	var data: CollisionEntityData
) : Event() {

	/** 是否记录本次命中（防重复触发），默认为 true */
	var isRecord: Boolean = true

	/**
	 * 碰撞检查事件 —— 在碰撞检测前触发。
	 *
	 * 实现 [ICancellableEvent]，取消后跳过该目标实体的后续碰撞处理。
	 * 可用于自定义过滤规则（如队伍检查、距离限制等）。
	 */
	class Check(
		attacker: Entity,
		colliderId: CollisionEntry,
		target: Entity,
		data: CollisionEntityData
	) : CollisionEntityEvent(attacker, colliderId, target, data), ICancellableEvent

	/**
	 * 碰撞命中事件 —— 碰撞检测通过后触发。
	 *
	 * 监听此事件以执行伤害、击退、状态效果等游戏逻辑。
	 * 不含任何默认伤害/击退逻辑，需由外部监听者处理。
	 */
	class Hit(
		attacker: Entity,
		colliderId: CollisionEntry,
		target: Entity,
		data: CollisionEntityData,
	) : CollisionEntityEvent(attacker, colliderId, target, data)
}
