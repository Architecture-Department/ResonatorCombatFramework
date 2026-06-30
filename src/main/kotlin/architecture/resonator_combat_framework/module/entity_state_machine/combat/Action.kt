package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

/**
 * 动作
 *
 * @param timing 前摇/执行/后摇时长
 * @param interruptData 打断配置
 */
@AllOpe
abstract class Action(
	val id: ResourceLocation,
	val timing: StageTiming,// TODO 基类不应该带有这个
	val interruptData: InterruptData,// TODO 基类不应该带有这个
	val weight: Int = 2500
) {
	val timeLength: Float = timing.windupSec + timing.activeSec + timing.recoverySec

	// ===== 生命周期钩子 =====

	/** 动作开始时调用 */
	fun onStart(entity: LivingEntity, actionSequence: ActionSequence?) {}

	/** 每 tick 调用 */
	fun onTick(entity: LivingEntity, actionSequence: ActionSequence?, time: Float) {}

	/** 进入前摇阶段时调用 */
	fun onWindup(entity: LivingEntity, actionSequence: ActionSequence?, time: Float) {}

	/** 进入执行阶段（攻击判定窗口）时调用 */
	fun onActive(entity: LivingEntity, actionSequence: ActionSequence?, time: Float) {}

	/** 进入后摇阶段时调用 */
	fun onRecovery(entity: LivingEntity, actionSequence: ActionSequence?, time: Float) {}

	/** 动作结束时调用 */
	fun onEnd(entity: LivingEntity, actionSequence: ActionSequence?) {}

	fun onSpeedModify(entity: LivingEntity, actionSequence: ActionSequence?, oldValue: Float, newValue: Float) {}

	// ===== 阶段查询 =====

	fun getState(time: Float, entity: LivingEntity): ActionState {
		return when {
			time < 0f -> ActionState.IDLE
			time < timing.windupSec -> ActionState.WINDUP
			time < timing.windupSec + timing.activeSec -> ActionState.ATTACK
			time < timeLength -> ActionState.RECOVERY
			else -> ActionState.IDLE
		}
	}

	fun isInterruptible(time: Float, holder: EntityStateHolder<*>, target: Action, entity: LivingEntity): Boolean {
		val actionState = getState(time, entity)
		val interruptWeight = interruptData.getInterruptWeight(actionState)
		if (interruptWeight < 0) {
			return RcfEventHooks.CombatActionInterruptible(holder, entity, this, target, false)
		}
		return RcfEventHooks.CombatActionInterruptible(holder, entity, this, target, interruptWeight < target.weight)
	}

	fun nextAction(
		time: Float,
		sourceIndex: Int,
		nextIndex: Int,
		actionSequence: ActionSequence?,
		entity: LivingEntity
	): Action? {
		actionSequence ?: return null
		return actionSequence.getAction(nextIndex)
	}

	fun nextAction(time: Float, sourceIndex: Int, actionSequence: ActionSequence?, entity: LivingEntity): Action? {
		return nextAction(time, sourceIndex, sourceIndex + 1, actionSequence, entity)
	}
}
