package architecture.resonator_combat_framework.event

import architecture.resonator_combat_framework.combat.ActionState
import architecture.resonator_combat_framework.state_machine.holder.EntityStateHolder
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.Event

/** 动作阶段切换事件 —— WINDUP / ACTIVE / RECOVERY / IDLE 之间转换时触发 */
class ActionStateChangedEvent(
	val holder: EntityStateHolder<*>,
	val entity: LivingEntity,
	val oldValue: ActionState,
	val newValue: ActionState,
) : Event()