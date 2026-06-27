package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.entity_state_machine.event.CombatActionEvent
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import architecture.resonator_combat_framework.util.TimeUtil
import net.minecraft.world.entity.LivingEntity
import kotlin.math.max

@AllOpe
class ActionController(val entity: LivingEntity) {
	private lateinit var _holder: EntityStateHolder<*>
	val holder: EntityStateHolder<*>
		get() {
			if (!::_holder.isInitialized) {
				_holder = entity.getData(RcfAttachmentTypes.STATE_HOLDER)
			}
			return _holder
		}

	/** 当前动作集 */
	final var actionData: ActionData? = null
		set(value) {
			stageIndex = 0
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
	final var stageIndex: Int = 0; private set

	/** 当前动作阶段 */
	final var actionState: ActionState = ActionState.EMPTY
		set(value) {
			if (field == value) return
			RcfEventHooks.CombatActionStateChanged(holder, entity, field, value)
			field = value
		}

	/** 本段已播放时间（秒） */
	final var time: Float = 0f; private set

	/** 战斗速度倍率 */
	final var combatSpeedMultiplier: Float = 1f
		set(value) {
			field = max(value, 0f)
		}

	/** 切换动作 */
	fun onChangedAction(target: Action): Boolean {
		if (action != null && !action!!.isInterruptible(time, holder, target, entity)) {
			return false
		}
		return onChangedAction(target, CombatActionEvent.Changed.Type.DEFAULT)
	}

	/** 强制切换动作 */
	fun onCompulsoryChangedAction(target: Action): Boolean {
		if (action != null && !action!!.isInterruptible(time, holder, target, entity)) {
			return onChangedAction(target, CombatActionEvent.Changed.Type.INTERRUPTIBLE)
		}
		return onChangedAction(target, CombatActionEvent.Changed.Type.DEFAULT)
	}

	/** 切换动作 */
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
	fun onActionEnd() {
		time = 0f
		combatSpeedMultiplier = 1f
		actionState = ActionState.EMPTY
		if (action != null) {
			RcfEventHooks.CombatActionEnd(holder, entity, action!!)
			onChangedAction(null, CombatActionEvent.Changed.Type.END)
		}
	}

	/** 切换下一段动作 */
	fun onNextAction() {
		var nextIndex = stageIndex + 1
		if ((actionData?.stages?.size ?: 0) <= nextIndex) {
			nextIndex = 0
		}
		onChangedAction(
			action?.nextAction(time, stageIndex, nextIndex, actionData, entity),
			CombatActionEvent.Changed.Type.NEXT
		)
	}

	fun tick() {
		val action = action ?: return
		val rawDelta = 1 / 20f
		val scaledDelta = TimeUtil.calcScaledDelta(rawDelta, combatSpeedMultiplier)

		if (scaledDelta <= 0f) return

		if (time <= 0f) {
			RcfEventHooks.CombatActionStart(holder, entity, action)
		}

		if (!RcfEventHooks.CombatActionTickPre(holder, entity, action)) {
			action.tick(time, actionData, entity)
			RcfEventHooks.CombatActionTickPost(holder, entity, action)
		}
		actionState = action.getState(time, entity)

		time += scaledDelta

		if (time >= action.timeLength) {
			onActionEnd()
			return
		}
	}
}