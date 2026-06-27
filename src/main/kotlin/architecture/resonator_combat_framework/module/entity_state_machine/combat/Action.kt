package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

/**
 * 单段攻击配置 —— 定义连击序列中的一段攻击。
 *
 * @param timing 前摇/执行/后摇时长
 * @param interruptData 打断配置
 */
@AllOpe
abstract class Action(
	val id: ResourceLocation,
	val timing: StageTiming,
	val interruptData: InterruptData,
	val weight: Int,
) {
	val timeLength: Float = timing.windupSec + timing.activeSec + timing.recoverySec

	fun tick(time: Float, actionSequence: ActionSequence?, entity: LivingEntity) {
	}

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
		val interruptWeight = interruptData.getInterruptWeight(getState(time, entity))
		if (interruptWeight < 0) {
			return RcfEventHooks.CombatActionInterruptible(holder, entity, this, target, false)
		}
		return RcfEventHooks.CombatActionInterruptible(holder, entity, this, target, interruptWeight < target.weight)
	}

	fun nextAction(time: Float, sourceIndex: Int, nextIndex: Int, actionSequence: ActionSequence?, entity: LivingEntity): Action? {
		actionSequence ?: return null
		return actionSequence.getStage(nextIndex)
	}

	fun nextAction(time: Float, sourceIndex: Int, actionSequence: ActionSequence?, entity: LivingEntity): Action? {
		return nextAction(time, sourceIndex, sourceIndex + 1, actionSequence, entity)
	}
}

