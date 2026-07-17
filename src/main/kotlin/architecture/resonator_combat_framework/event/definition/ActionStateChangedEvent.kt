package architecture.resonator_combat_framework.event.definition

import architecture.resonator_combat_framework.combat.ActionController
import architecture.resonator_combat_framework.combat.ActionState
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.Event

/** 动作阶段切换事件 —— WINDUP / ACTIVE / RECOVERY / IDLE 之间转换时触发 */
class ActionStateChangedEvent(
	val controller: ActionController,
	val entity: LivingEntity,
	val oldValue: ActionState,
	val newValue: ActionState,
) : Event()