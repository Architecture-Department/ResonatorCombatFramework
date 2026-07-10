package architecture.resonator_combat_framework.module.state_machine.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.combat.ActionState
import architecture.resonator_combat_framework.module.state_machine.holder.EntityStateHolder
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.Event

/**
 * 基础战斗事件 —— 由 [ActionController] 在动作阶段切换时触发，供外部模块监听处理。
 *
 * 当前仅包含 [ActionStateChanged] 事件，在动作的阶段（WINDUP/ACTIVE/RECOVERY）切换时发射。
 *
 * 外部监听示例：
 * ```
 * NeoForge.EVENT_BUS.addListener<CombatEvent.ActionStateChanged> { event ->
 *     when (event.newValue) {
 *         ActionState.WINDUP -> // 前摇处理
 *         ActionState.ACTIVE -> // 执行处理
 *         ActionState.RECOVERY -> // 后摇处理
 *     }
 * }
 * ```
 */
@AllOpe
abstract class CombatEvent(
	val holder: EntityStateHolder<*>,
	val entity: LivingEntity,
) : Event() {

	/** 动作阶段切换事件 —— WINDUP / ACTIVE / RECOVERY / IDLE 之间转换时触发 */
	class ActionStateChanged(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		val oldValue: ActionState,
		val newValue: ActionState,
	) : CombatEvent(holder, entity)
}
