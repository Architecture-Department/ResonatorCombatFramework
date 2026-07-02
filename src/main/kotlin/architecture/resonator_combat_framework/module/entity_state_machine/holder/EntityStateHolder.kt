package architecture.resonator_combat_framework.module.entity_state_machine.holder

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionController
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
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

		/** 冲刺中 */
		@JvmField
		val SPRINTING_STATE = RcfUtil.modRl("sprint")

		/** 挥动攻击中 */
		@JvmField
		val ATTACK_STATE = RcfUtil.modRl("attack")

		/** 睡觉中 */
		@JvmField
		val SLEEPING_STATE = RcfUtil.modRl("sleeping")

		/** 正在使用物品（拉弓、吃食物等） */
		@JvmField
		val USING_ITEM_STATE = RcfUtil.modRl("using_item")

		/** 吃食物中 */
		@JvmField
		val EATING_STATE = RcfUtil.modRl("eating")

		/** 能否切换物品（由 ActionAnimation 控制） */
		@JvmField
		val CAN_SWITCH_ITEM = RcfUtil.modRl("can_switch_item")

		/** 能否移动（由攻击动画控制） */
		@JvmField
		val CAN_MOVE = RcfUtil.modRl("can_move")

		/** 能否转动视角（由攻击动画控制） */
		@JvmField
		val CAN_LOOK_AROUND = RcfUtil.modRl("can_look_around")

		/** 移动速度倍率（0=不能移动，1=正常速度），float 状态 */
		@JvmField
		val SPEED_MODIFIER = RcfUtil.modRl("speed_modifier")

		/** 最大视角转动速度（弧度/秒），float 状态 */
		@JvmField
		val MAX_LOOK_SPEED = RcfUtil.modRl("max_look_speed")

		/** 被骑乘中（它人骑在身上） */
		@JvmField
		val VEHICLE_STATE = RcfUtil.modRl("vehicle")

		/** 骑乘中（骑在别人身上） */
		@JvmField
		val PASSENGER_STATE = RcfUtil.modRl("passenger")

		/** 在地面上 */
		@JvmField
		val ON_GROUND_STATE = RcfUtil.modRl("on_ground")

		/** 在岩浆中 */
		@JvmField
		val IN_LAVA_STATE = RcfUtil.modRl("in_lava")

		/** 在水中 */
		@JvmField
		val IN_WATER_STATE = RcfUtil.modRl("in_water")

		/** 爬行中 */
		@JvmField
		val CRAWLING_STATE = RcfUtil.modRl("crawling")

		/** 蹲下姿态中 */
		@JvmField
		val CROUCHING_STATE = RcfUtil.modRl("crouching")

		/** Pose: 站立 */
		@JvmField
		val POSE_STANDING = RcfUtil.modRl("pose_standing")

		/** Pose: 鞘翅飞行 */
		@JvmField
		val POSE_FALL_FLYING = RcfUtil.modRl("pose_fall_flying")

		/** Pose: 睡觉 */
		@JvmField
		val POSE_SLEEPING = RcfUtil.modRl("pose_sleeping")

		/** Pose: 游泳 */
		@JvmField
		val POSE_SWIMMING = RcfUtil.modRl("pose_swimming")

		/** Pose: 旋转攻击 */
		@JvmField
		val POSE_SPIN_ATTACK = RcfUtil.modRl("pose_spin_attack")

		/** Pose: 蹲下 */
		@JvmField
		val POSE_CROUCHING = RcfUtil.modRl("pose_crouching")

		/** Pose: 长跑 */
		@JvmField
		val POSE_LONG_JUMPING = RcfUtil.modRl("pose_long_jumping")

		/** Pose: 死亡 */
		@JvmField
		val POSE_DYING = RcfUtil.modRl("pose_dying")

		/** Pose: 坐着 */
		@JvmField
		val POSE_SITTING = RcfUtil.modRl("pose_sitting")

		/** Pose: 咆哮 */
		@JvmField
		val POSE_ROARING = RcfUtil.modRl("pose_roaring")

		/** Pose: 嗅探 */
		@JvmField
		val POSE_SNIFFING = RcfUtil.modRl("pose_sniffing")

		/** Pose: 钻出 */
		@JvmField
		val POSE_EMERGING = RcfUtil.modRl("pose_emerging")

		/** Pose: 挖掘 */
		@JvmField
		val POSE_DIGGING = RcfUtil.modRl("pose_digging")

		/** Pose: 滑行 */
		@JvmField
		val POSE_SLIDING = RcfUtil.modRl("pose_sliding")

		/** Pose: 射击 */
		@JvmField
		val POSE_SHOOTING = RcfUtil.modRl("pose_shooting")

		/** Pose: 吸气 */
		@JvmField
		val POSE_INHALING = RcfUtil.modRl("pose_inhaling")
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
		setState(SPRINTING_STATE, entity.isSprinting)
		setState(ATTACK_STATE, entity.swinging)
		setState(SLEEPING_STATE, entity.isSleeping)
		setState(USING_ITEM_STATE, entity.isUsingItem)
		setState(EATING_STATE, entity.isUsingItem && entity.useItem.get(DataComponents.FOOD) != null)
		setState(VEHICLE_STATE, entity.isVehicle)
		setState(PASSENGER_STATE, entity.isPassenger)
		setState(ON_GROUND_STATE, entity.onGround())
		setState(IN_LAVA_STATE, entity.isInLava)
		setState(IN_WATER_STATE, entity.isInWater)
		setState(CRAWLING_STATE, entity.isVisuallyCrawling)
		setState(CROUCHING_STATE, entity.isCrouching)
		setState(POSE_STANDING, entity.pose == Pose.STANDING)
		setState(POSE_FALL_FLYING, entity.pose == Pose.FALL_FLYING)
		setState(POSE_SLEEPING, entity.pose == Pose.SLEEPING)
		setState(POSE_SWIMMING, entity.pose == Pose.SWIMMING)
		setState(POSE_SPIN_ATTACK, entity.pose == Pose.SPIN_ATTACK)
		setState(POSE_CROUCHING, entity.pose == Pose.CROUCHING)
		setState(POSE_LONG_JUMPING, entity.pose == Pose.LONG_JUMPING)
		setState(POSE_DYING, entity.pose == Pose.DYING)
		setState(POSE_SITTING, entity.pose == Pose.SITTING)
		setState(POSE_ROARING, entity.pose == Pose.ROARING)
		setState(POSE_SNIFFING, entity.pose == Pose.SNIFFING)
		setState(POSE_EMERGING, entity.pose == Pose.EMERGING)
		setState(POSE_DIGGING, entity.pose == Pose.DIGGING)
		setState(POSE_SLIDING, entity.pose == Pose.SLIDING)
		setState(POSE_SHOOTING, entity.pose == Pose.SHOOTING)
		setState(POSE_INHALING, entity.pose == Pose.INHALING)
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
	 * 获取移动动画触发阈值。
	 * 当实体的水平平均速度超过此值时触发移动状态。
	 * @return 速度阈值
	 */
	fun getMotionAnimThreshold(): Float = 0.015f
}
