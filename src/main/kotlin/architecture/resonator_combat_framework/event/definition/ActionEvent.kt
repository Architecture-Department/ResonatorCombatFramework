package architecture.resonator_combat_framework.event.definition

import architecture.resonator_combat_framework.combat.Action
import architecture.resonator_combat_framework.combat.ActionController
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

/**
 * 战斗动作事件 —— 在 [ActionController] 中动作生命周期的各个节点触发。
 * 包含开始、结束、切换、打断判定和 tick 前/后等子事件。
 *
 * @param controller 实体状态持有者
 * @param entity 事件关联的生物实体
 * @param action 事件关联的动作
 */
abstract class ActionEvent(
	val controller: ActionController,
	val entity: LivingEntity,
	val action: Action?,
) : Event() {
	/** 开始 */
	class Start(
		controller: ActionController,
		entity: LivingEntity,
		action: Action,
	) : ActionEvent(controller, entity, action)

	/** 结束 */
	class End(
		controller: ActionController,
		entity: LivingEntity,
		action: Action
	) : ActionEvent(controller, entity, action)

	/** 切换 */
	class Changed(
		controller: ActionController,
		entity: LivingEntity,
		@Suppress("CanBeParameter", "RedundantSuppression")
		val oldValue: Action?,
		var newValue: Action?,
		val type: Type
	) : ActionEvent(controller, entity, oldValue), ICancellableEvent {
		enum class Type {
			DEFAULT,
			NEXT,
			INTERRUPTIBLE,
			END
		}
	}

	/** 打断 */
	class Interruptible(
		controller: ActionController,
		entity: LivingEntity,
		action: Action,
		val target: Action,
		val oldValue: Boolean,
		var newValue: Boolean,
	) : ActionEvent(controller, entity, action), ICancellableEvent

	abstract class Tick(
		controller: ActionController,
		entity: LivingEntity,
		action: Action
	) : ActionEvent(controller, entity, action) {
		class Pre(
			controller: ActionController,
			entity: LivingEntity,
			action: Action
		) : Tick(controller, entity, action), ICancellableEvent

		class Post(
			controller: ActionController,
			entity: LivingEntity,
			action: Action
		) : Tick(controller, entity, action)
	}
}