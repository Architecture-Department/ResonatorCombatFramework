package architecture.resonator_combat_framework.state_machine.holder

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.combat.ActionController
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/**
 * 实体状态持有者 —— 存储并管理实体的运行时状态标志和动作控制器。
 * 维护一组以 [ResourceLocation] 为键的布尔状态（如是否移动、攻击、游泳等），
 * 持有 [ActionController] 驱动战斗动作。
 *
 * @param T 实体类型
 * @param entity 所属实体
 * @param actionController 动作控制器
 */
@AllOpe
class EntityStateHolder<T : LivingEntity>(
	val entity: T,
	val actionController: ActionController = ActionController(entity)
) {
	init {
		actionController.holder = this
	}

	companion object {
		/** 移动中（速度超过阈值且脚步动画中） */
		@JvmField
		val MOVE_STATE = RcfUtil.modRl("move")

		/** 吃食物中 */
		@JvmField
		val EATING_STATE = RcfUtil.modRl("eating")
	}

	/** 实体状态映射表 */
	private val states = mutableMapOf<ResourceLocation, Boolean>()

	/** 实体浮点状态映射表（速度倍率、视角速度等） */
	private val floatStates = mutableMapOf<ResourceLocation, Float>()

	/**
	 * 每 tick 更新状态标志和动作控制器。
	 */
	fun tick() {
		tickStates()
		actionController.tick()
	}

	/**
	 * 更新所有预定义状态标志。
	 * 根据实体的移动量、姿态、骑乘、物品使用等条件，
	 * 同步设置 MOVE_STATE、SPRINTING_STATE 等状态标志。
	 */
	fun tickStates() {
		val limbSwingAmount = entity.walkAnimation.speed(1f)
		val velocity: Vec3 = entity.deltaMovement
		val avgVelocity = ((abs(velocity.x) + abs(velocity.z)) / 2f).toFloat()
		val motionThreshold: Float = getMotionAnimThreshold()
		setState(MOVE_STATE, avgVelocity >= motionThreshold && limbSwingAmount != 0f)
		setState(EATING_STATE, entity.isUsingItem && entity.useItem.get(DataComponents.FOOD) != null)
	}

	/**
	 * 检查是否包含指定状态。
	 * @param id 状态标识符
	 * @return 是否存在该状态
	 */
	fun containState(id: ResourceLocation): Boolean = states.containsKey(id)

	/**
	 * 获取指定状态的值。
	 * @param id 状态标识符
	 * @return 状态值，未设置时返回 false
	 */
	fun getState(id: ResourceLocation): Boolean = states[id] ?: false

	/**
	 * 设置指定状态的值。
	 * @param id 状态标识符
	 * @param value 状态值
	 */
	fun setState(id: ResourceLocation, value: Boolean) {
		states[id] = value
	}

	// ===== 浮点状态管理 =====

	/**
	 * 获取指定浮点状态的值。
	 * @param id 状态标识符
	 * @return 浮点状态值，未设置时返回 0f
	 */
	fun getFloatState(id: ResourceLocation): Float = floatStates[id] ?: 0f

	/**
	 * 设置指定浮点状态的值。
	 * @param id 状态标识符
	 * @param value 浮点状态值
	 */
	fun setFloatState(id: ResourceLocation, value: Float) {
		floatStates[id] = value
	}

	/**
	 * 获取所有浮点状态的只读视图。
	 * @return 浮点状态映射表
	 */
	fun getFloatStates(): Map<ResourceLocation, Float> = floatStates

	/**
	 * 获取所有状态的只读视图。
	 * @return 状态映射表
	 */
	fun getStates(): Map<ResourceLocation, Boolean> = states

	/**
	 * 清除所有状态标志。
	 */
	fun clearStates() {
		states.clear()
	}

	/**
	 * 批量应用外部状态修饰。
	 * 由动画系统在 onStart 时调用，设置 CAN_MOVE、CAN_LOOK_AROUND 等状态。
	 * @param modifiers 状态映射表（key=状态ID, value=目标布尔值）
	 */
	fun applyStateModifiers(modifiers: Map<ResourceLocation, Boolean>) {
		states.putAll(modifiers)
	}

	/**
	 * 批量应用外部浮点状态修饰。
	 * 由动画系统在 onStart 时调用，设置 SPEED_MODIFIER、MAX_LOOK_SPEED 等状态。
	 * @param modifiers 浮点状态映射表（key=状态ID, value=目标浮点值）
	 */
	fun applyFloatModifiers(modifiers: Map<ResourceLocation, Float>) {
		floatStates.putAll(modifiers)
	}

	/**
	 * 获取移动动画触发阈值。
	 * 当实体的水平平均速度超过此值时触发移动状态。
	 * @return 速度阈值
	 */
	fun getMotionAnimThreshold(): Float = 0.015f
}
