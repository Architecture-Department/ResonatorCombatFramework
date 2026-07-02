package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.module.entity_state_machine.event.CombatActionEvent
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import architecture.resonator_combat_framework.util.TimeUtil
import net.minecraft.world.entity.LivingEntity
import kotlin.math.max

/**
 * 动作控制器 —— 管理实体战斗动作的完整生命周期。
 * 负责驱动 Action 的阶段推进、tick 更新、动作切换与打断判定。
 * 每个 [EntityStateHolder] 持有一个 ActionController 实例。
 *
 * @param entity 所属生物实体
 */
@AllOpe
class ActionController(val entity: LivingEntity) {
	/** 状态持有者引用（由 EntityStateHolder 在构造后立即设置） */
	final lateinit var holder: EntityStateHolder<*>
		internal set

	/** 当前动作集 */
	* 设置时自动重置舞台索引为 -1，并将动作阶段设为 WINDUP。
	final var actionSequence: ActionSequence? = null
		set(value) {
			stageIndex = -1
			if (value == null) {
				actionState = ActionState.EMPTY
				return
			}
			actionState = ActionState.WINDUP
			field = value
		}

	/** 当前动作 */
	final var action: Action? = null; private set

	/** 当前连击段索引 */
	final var stageIndex: Int = -1; private set

	/** 当前动作阶段 */
	* 设置时若值发生变化，发射 [CombatEvent.ActionStateChanged] 事件。
	final var actionState: ActionState = ActionState.EMPTY
		set(value) {
			if (field == value) return
			RcfEventHooks.CombatActionStateChanged(holder, entity, field, value)
			field = value
		}

	/** 本段已播放时间（秒） */
	final var time: Float = 0f; private set

	/** 上一 tick 的动作阶段，用于检测阶段切换 */
	private var prevActionState: ActionState = ActionState.EMPTY

	/** 战斗速度倍率 */
	* 设置时若值变化，回调 [Action.onSpeedModify]。最小值限制为 0。
	final var combatSpeedMultiplier: Float = 1f
		set(value) {
			val newValue = max(value, 0f)
			action?.onSpeedModify(entity, actionSequence, field, newValue)
			field = newValue
		}

	/** 切换动作 */
	/**
	 * 切换动作，若当前有动作则先检查打断性。
	 *
	 * @param target 目标动作
	 * @return 是否成功切换
	 */
	fun onChangedAction(target: Action): Boolean {
		if (action != null) {
			if (!action!!.isInterruptible(time, holder, target, entity)) return false
		}
		return onChangedAction(target, CombatActionEvent.Changed.Type.DEFAULT)
	}

	/** 切换动作 */
	/**
	 * 切换到下一段动作（NEXT 类型）。
	 *
	 * @param target 目标动作
	 * @return 是否成功切换
	 */
	fun onNextChangedAction(target: Action): Boolean {
		if (action != null) {
			if (!action!!.isInterruptible(time, holder, target, entity)) return false
		}
		return onChangedAction(target, CombatActionEvent.Changed.Type.NEXT)
	}

	/** 强制切换动作 */
	/**
	 * 强制切换动作。若当前动作不可打断则使用 INTERRUPTIBLE 类型强制切换。
	 *
	 * @param target 目标动作
	 * @return 是否成功切换
	 */
	fun onCompulsoryChangedAction(target: Action): Boolean {
		if (action != null) {
			if (!action!!.isInterruptible(time, holder, target, entity)) {
				return onChangedAction(target, CombatActionEvent.Changed.Type.INTERRUPTIBLE)
			}
		}
		return onChangedAction(target, CombatActionEvent.Changed.Type.DEFAULT)
	}

	/** 切换动作 */
	/**
	 * 切换动作的内部实现。发射 [CombatActionEvent.Changed] 事件并检查是否被取消。
	 *
	 * @param value 目标动作
	 * @param type 切换类型
	 * @return 是否成功切换
	 */
	private fun onChangedAction(value: Action?, type: CombatActionEvent.Changed.Type): Boolean {
		val event = RcfEventHooks.CombatActionChanged(holder, entity, action, value, type)
		if (event.isCanceled) {
			return false
		}
		time = 0f
		action = event.newValue
		return true
	}

	/** 动作结束 */
	/**
	 * 结束当前动作。重置时间和速度倍率，调用 onEnd 回调并发射结束事件。
	 */
	fun onActionEnd() {
		time = 0f
		combatSpeedMultiplier = 1f
		actionState = ActionState.EMPTY
		if (action != null) {
			action!!.onEnd(entity, actionSequence)
			RcfEventHooks.CombatActionEnd(holder, entity, action!!)
			onChangedAction(null, CombatActionEvent.Changed.Type.END)
		}
	}

	/** 切换下一段动作 */
	/**
	 * 切换到动作序列的下一段。若当前为末段则回到首段。
	 *
	 * @return 是否成功切换
	 */
	fun onNextAction(): Boolean {
		var nextIndex = stageIndex + 1
		if ((actionSequence?.stages?.size ?: 0) <= nextIndex) {
			nextIndex = 0
		}
		val target = if (action != null) {
			action!!.nextAction(time, stageIndex, nextIndex, actionSequence, entity) ?: return false
		} else {
			actionSequence?.getAction(nextIndex) ?: return false
		}
		val isSuccess = onNextChangedAction(target)
		if (isSuccess) {
			stageIndex = if (actionSequence != null) {
				nextIndex % (actionSequence!!.stages.size)
			} else {
				nextIndex
			}
		}
		return isSuccess
	}

	/** 强制切换下一段动作 */
	/**
	 * 强制切换到动作序列的下一段，无视打断规则。
	 *
	 * @return 是否成功切换
	 */
	fun onCompulsoryNextAction(): Boolean {
		var nextIndex = stageIndex + 1
		if ((actionSequence?.stages?.size ?: 0) <= nextIndex) {
			nextIndex = 0
		}
		val target = if (action != null) {
			action!!.nextAction(time, stageIndex, nextIndex, actionSequence, entity) ?: return false
		} else {
			actionSequence?.getAction(nextIndex) ?: return false
		}
		val isSuccess = onCompulsoryChangedAction(target)
		if (isSuccess) {
			stageIndex = nextIndex
		}
		return isSuccess
	}


	/**
	 * 每 tick 驱动动作推进。
	 * 计算缩放后的时间增量，调用动作生命周期回调，检测阶段切换，并在动作结束时自动重置。
	 */
	fun tick() {
		val action = action ?: return
		val rawDelta = 1 / 20f
		val scaledDelta = TimeUtil.calcScaledDelta(rawDelta, combatSpeedMultiplier)

		if (scaledDelta <= 0f) return

		if (time <= 0f) {
			RcfEventHooks.CombatActionStart(holder, entity, action)
			action.onStart(entity, actionSequence)
		}
		if (!RcfEventHooks.CombatActionTickPre(holder, entity, action)) {
			action.onTick(entity, actionSequence, time)
			RcfEventHooks.CombatActionTickPost(holder, entity, action)
		}
		actionState = action.getState(time, entity)
		// 阶段切换回调
		if (actionState != prevActionState) {
			when (actionState) {
				ActionState.WINDUP -> action.onWindup(entity, actionSequence, time)
				ActionState.ACTIVE -> action.onActive(entity, actionSequence, time)
				ActionState.RECOVERY -> action.onRecovery(entity, actionSequence, time)
				else -> {}
			}
			prevActionState = actionState
		}

		time += scaledDelta

		if (time >= action.timeLength) {
			onActionEnd()
			stageIndex = -1
			return
		}
	}
}
