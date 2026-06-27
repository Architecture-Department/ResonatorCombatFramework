package architecture.resonator_combat_framework.module.entity_state_machine.event

import architecture.resonator_combat_framework.module.entity_state_machine.combat.Action
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.ICancellableEvent

/** 动作 */
abstract class CombatActionEvent(
	holder: EntityStateHolder<*>,
	entity: LivingEntity,
	val action: Action?,
) : CombatEvent(holder, entity) {
	enum class Reason {
		/** 正常 */
		NORMAL,

		/** 切换 */
		SWITCH,

		/** 触发下一段动作 */
		NEXT,

		/** 强制 */
		COMPULSORY,
	}

	/** 开始 */
	class Start(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		action: Action,
	) : CombatActionEvent(holder, entity, action)

	/** 结束 */
	class End(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		action: Action
	) : CombatActionEvent(holder, entity, action)

	/** 切换 */
	class Changed(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		@Suppress("CanBeParameter", "RedundantSuppression")
		val oldValue: Action?,
		var newValue: Action?,
		val type: Type
	) : CombatActionEvent(holder, entity, oldValue), ICancellableEvent {
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
	) : CombatActionEvent(holder, entity, action), ICancellableEvent

	abstract class Tick(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		action: Action
	) : CombatActionEvent(holder, entity, action) {
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