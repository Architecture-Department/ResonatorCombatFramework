package architecture.resonator_combat_framework.event

import architecture.resonator_combat_framework.combat.Action
import architecture.resonator_combat_framework.state_machine.holder.EntityStateHolder
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

/**
 * 战斗动作事件 —— 在 [ActionController] 中动作生命周期的各个节点触发。
 * 包含开始、结束、切换、打断判定和 tick 前/后等子事件。
 *
 * @param holder 实体状态持有者
 * @param entity 事件关联的生物实体
 * @param action 事件关联的动作
 */
abstract class ActionEvent(
	val holder: EntityStateHolder<*>,
	val entity: LivingEntity,
	val action: Action?,
) : Event() {
	/** 开始 */
	class Start(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		action: Action,
	) : ActionEvent(holder, entity, action)

	/** 结束 */
	class End(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		action: Action
	) : ActionEvent(holder, entity, action)

	/** 切换 */
	class Changed(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		@Suppress("CanBeParameter", "RedundantSuppression")
		val oldValue: Action?,
		var newValue: Action?,
		val type: Type
	) : ActionEvent(holder, entity, oldValue), ICancellableEvent {
		enum class Type {
			DEFAULT,
			NEXT,
			INTERRUPTIBLE,
			END
		}
	}

	/** 打断 */
	class Interruptible(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		action: Action,
		val target: Action,
		val oldValue: Boolean,
		var newValue: Boolean,
	) : ActionEvent(holder, entity, action), ICancellableEvent

	abstract class Tick(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		action: Action
	) : ActionEvent(holder, entity, action) {
		class Pre(
			holder: EntityStateHolder<*>,
			entity: LivingEntity,
			action: Action
		) : Tick(holder, entity, action), ICancellableEvent

		class Post(
			holder: EntityStateHolder<*>,
			entity: LivingEntity,
			action: Action
		) : Tick(holder, entity, action)
	}
}