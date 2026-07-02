package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

/**
 * 动作 —— 定义一次攻击动作的完整生命周期，包含时长、阶段判定、打断规则和各阶段回调。
 *
 * 每个动作包含前摇（WINDUP）、执行（ACTIVE）、后摇（RECOVERY）、空闲（IDLE）四个阶段。
 * 通过 [getState] 方法根据时间轴判定当前所处阶段，在 [ActionController.tick] 中驱动阶段切换。
 *
 * @param id 动作的唯一标识符
 * @param durationTick 动作持续时长（游戏刻）
 * @param interruptData 打断配置，定义各阶段的可打断性
 * @param weight 动作权重，用于打断判定时与目标动作权重比较
 */
@AllOpe
abstract class Action(
	val id: ResourceLocation,
	val durationTick: Int,
	val interruptData: InterruptData,
	val weight: Int = 2500
) {
	val timeLength = durationTick / 20f

	fun getState(time: Float, entity: LivingEntity): ActionState {
		return when {
			time < 0f -> ActionState.IDLE
			time < durationTick -> ActionState.ACTIVE
			else -> ActionState.IDLE
		}
	}

	// ===== 生命周期钩子 =====

	/**
	 * 动作开始时调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 */
	fun onStart(entity: LivingEntity, actionSequence: ActionSequence?) {}

	/**
	 * 每 tick 调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 * @param time 当前已播放时间（秒）
	 */
	fun onTick(entity: LivingEntity, actionSequence: ActionSequence?, time: Float) {}

	/**
	 * 进入前摇阶段时调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 * @param time 当前已播放时间（秒）
	 */
	fun onWindup(entity: LivingEntity, actionSequence: ActionSequence?, time: Float) {}

	/**
	 * 进入执行阶段（攻击判定窗口）时调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 * @param time 当前已播放时间（秒）
	 */
	fun onActive(entity: LivingEntity, actionSequence: ActionSequence?, time: Float) {}

	/**
	 * 进入后摇阶段时调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 * @param time 当前已播放时间（秒）
	 */
	fun onRecovery(entity: LivingEntity, actionSequence: ActionSequence?, time: Float) {}

	/**
	 * 动作结束时调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 */
	fun onEnd(entity: LivingEntity, actionSequence: ActionSequence?) {}

	/**
	 * 战斗速度倍率变化时调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 * @param oldValue 原速度倍率
	 * @param newValue 新速度倍率
	 */
	fun onSpeedModify(entity: LivingEntity, actionSequence: ActionSequence?, oldValue: Float, newValue: Float) {}

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

	override fun toString(): String {
		return "id=$id"
	}
}
