package architecture.resonator_combat_framework.combat

import architecture.goldenboughs_lib.api.AllOpen
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.state.EntityStateHolder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import java.util.*

@AllOpen
class Action(
	/**
	 * 动作id
	 */
	val id: ResourceLocation,
	/**
	 * 动作持续时长（秒）
	 */
	val durationTimeLength: Float,
	/**
	 * 打断配置，定义各阶段的可打断性
	 */
	val interruptData: InterruptData,
	/**
	 * 动作权重，用于打断判定时与目标动作权重比较
	 */
	val weight: Int = 2500
) {
	/** 动作属性映射表 */
	val properties = mutableMapOf<ActionProperty<*>, Any>()

	fun isDuration(time: Float): Boolean {
		return 0f < time && time <= durationTimeLength
	}

	fun isStart(time: Float): Boolean {
		return time > 0
	}

	fun isEnd(time: Float): Boolean {
		return time > 0 && time > durationTimeLength
	}

	/**
	 * 链式添加动作属性。
	 * @param key 属性键
	 * @param value 属性值
	 * @return 自身，支持链式调用
	 */
	@Suppress("UNCHECKED_CAST")
	fun <T : Any> addProperty(key: ActionProperty<T>, value: T): Action {
		properties[key] = value as Any
		return this
	}

	/**
	 * 获取指定键的动作属性。
	 * @param key 属性键
	 * @return 属性值的 Optional 包装
	 */
	@Suppress("UNCHECKED_CAST")
	fun <T : Any> getProperty(key: ActionProperty<T>): Optional<T> = Optional.ofNullable((properties[key] as? T))

	fun getState(time: Float, entity: LivingEntity): ActionState {
		return when {
			time < 0f -> ActionState.IDLE
			time < durationTimeLength -> ActionState.ACTIVE
			else -> ActionState.IDLE
		}
	}

	/** 获取内部属性映射的只读视图 */
	fun getAllProperties(): Map<ActionProperty<*>, Any> = properties.toMap()

	/** 获取所有布尔状态修饰 */
	@Suppress("UNCHECKED_CAST")
	fun getStateModifiers(): Map<BooleanStateProperty, Pair<Boolean, Boolean>> =
		properties.filterKeys { it is BooleanStateProperty }.mapKeys { it.key as BooleanStateProperty }
			.mapValues { it.value as Pair<Boolean, Boolean> }

	/** 获取所有浮点状态修饰 */
	@Suppress("UNCHECKED_CAST")
	fun getFloatModifiers(): Map<FloatStateProperty, Pair<Float, Float>> =
		properties.filterKeys { it is FloatStateProperty }.mapKeys { it.key as FloatStateProperty }
			.mapValues { it.value as Pair<Float, Float> }

	@Suppress("DuplicatedCode")
	fun resetState(entity: LivingEntity) {
		if (!EntityStateHolder.has(entity)) return
		val stateHolder = EntityStateHolder.of(entity)
		val boolMods = getStateModifiers()
		if (boolMods.isNotEmpty()) {
			stateHolder.applyStateModifiers(boolMods.mapKeys { it.key.id }
				.mapValues { it.value.second })
		}
		val floatMods = getFloatModifiers()
		if (floatMods.isNotEmpty()) {
			stateHolder.applyFloatModifiers(floatMods.mapKeys { it.key.id }
				.mapValues { it.value.second })
		}
	}


	@Suppress("DuplicatedCode")
	fun applyState(entity: LivingEntity) {
		if (!EntityStateHolder.has(entity)) return
		val stateHolder = EntityStateHolder.of(entity)
		val boolMods = getStateModifiers()
		if (boolMods.isNotEmpty()) {
			stateHolder.applyStateModifiers(boolMods.mapKeys { it.key.id }
				.mapValues { it.value.second })
		}
		val floatMods = getFloatModifiers()
		if (floatMods.isNotEmpty()) {
			stateHolder.applyFloatModifiers(floatMods.mapKeys { it.key.id }
				.mapValues { it.value.second })
		}
	}

	// ===== 生命周期钩子 =====

	/**
	 * 动作开始时调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 */
	fun onStart(entity: LivingEntity, actionController: ActionController, actionSequence: ActionSequence?) {}

	/**
	 * 每 tick 调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 * @param time 当前已播放时间（秒）
	 */
	fun onTick(entity: LivingEntity, actionController: ActionController, actionSequence: ActionSequence?, time: Float) {}

	/**
	 * 进入前摇阶段时调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 * @param time 当前已播放时间（秒）
	 */
	fun onWindup(
		entity: LivingEntity, actionController: ActionController, actionSequence: ActionSequence?, time: Float
	) {
	}

	/**
	 * 进入执行阶段（攻击判定窗口）时调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 * @param time 当前已播放时间（秒）
	 */
	fun onActive(
		entity: LivingEntity, actionController: ActionController, actionSequence: ActionSequence?, time: Float
	) {
	}

	/**
	 * 进入后摇阶段时调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 * @param time 当前已播放时间（秒）
	 */
	fun onRecovery(
		entity: LivingEntity, actionController: ActionController, actionSequence: ActionSequence?, time: Float
	) {
	}

	/**
	 * 动作结束时调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 */
	fun onEnd(entity: LivingEntity, actionController: ActionController, actionSequence: ActionSequence?) {}

	/**
	 * 战斗速度倍率变化时调用。
	 *
	 * @param entity 执行动作的生物
	 * @param actionSequence 所属动作序列
	 * @param oldValue 原速度倍率
	 * @param newValue 新速度倍率
	 */
	fun onSpeedModify(
		entity: LivingEntity,
		actionController: ActionController,
		actionSequence: ActionSequence?,
		oldValue: Float,
		newValue: Float
	) {
	}

	fun isInterruptible(controller: ActionController, time: Float, target: Action, entity: LivingEntity): Boolean {
		val actionState = getState(time, entity)
		val interruptWeight = interruptData.getInterruptWeight(actionState)
		if (interruptWeight < 0) {
			return RcfEventHooks.combatActionInterruptible(controller, entity, this, target, false)
		}
		return RcfEventHooks.combatActionInterruptible(controller, entity, this, target, interruptWeight < target.weight)
	}

	fun nextAction(
		time: Float, sourceIndex: Int, nextIndex: Int, actionSequence: ActionSequence?, entity: LivingEntity
	): Action? {
		actionSequence ?: return null
		return actionSequence.getAction(nextIndex)
	}

	fun nextAction(time: Float, sourceIndex: Int, actionSequence: ActionSequence?, entity: LivingEntity): Action? {
		return nextAction(time, sourceIndex, sourceIndex + 1, actionSequence, entity)
	}

	override fun toString(): String {
		return "Action(id=$id)"
	}

	fun isMove(time: Float, controller: ActionController, entity: LivingEntity): Boolean {
		return true
	}

	fun isCanLookAround(time: Float, controller: ActionController, entity: LivingEntity): Boolean {
		return true
	}

	fun getSpeedModifier(time: Float, controller: ActionController, entity: LivingEntity): Float {
		return -1f
	}

	fun getMaxLookSpeed(time: Float, controller: ActionController, entity: LivingEntity): Float {
		return -1f
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Action) return false

		if (id != other.id) return false

		return true
	}

	override fun hashCode(): Int {
		return id.hashCode()
	}
}
