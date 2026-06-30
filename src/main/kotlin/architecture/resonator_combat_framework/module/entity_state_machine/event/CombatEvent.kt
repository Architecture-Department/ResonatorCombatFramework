package architecture.resonator_combat_framework.module.entity_state_machine.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionState
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.Event

/**
 * 战斗事件 —— 由 [CombatController] 在状态转换时触发，供外部模块处理。
 *
 * 外部监听示例：
 * ```
 * NeoForge.EVENT_BUS.addListener<CombatEvent.PhaseChanged> { event ->
 *     when (event.newPhase) {
 *         ActionPhase.WINDUP -> playAnimation(event.stage.animId)
 *         ActionPhase.ACTIVE -> writeColliders(event.entity, event.stage)
 *         ActionPhase.RECOVERY -> clearColliders(event.stage.id)
 *     }
 * }
 * ```
 */
@AllOpe
abstract class CombatEvent(
	val holder: EntityStateHolder<*>,
	val entity: LivingEntity,
) : Event() {

	/** 动作阶段切换 (WINDUP / ACTIVE / RECOVERY / IDLE) */
	class ActionStateChanged(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		val oldValue: ActionState,
		val newValue: ActionState,
	) : CombatEvent(holder, entity)
}