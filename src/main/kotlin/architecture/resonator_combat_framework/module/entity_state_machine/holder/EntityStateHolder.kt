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

	private val states = mutableMapOf<ResourceLocation, Boolean>()

	fun tick() {
		tickStates()
		actionController.tick()
	}

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

	fun containState(id: ResourceLocation): Boolean = states.containsKey(id)
	fun getState(id: ResourceLocation): Boolean = states[id] ?: false
	fun setState(id: ResourceLocation, value: Boolean) {
		states[id] = value
	}

	fun getStates(): Map<ResourceLocation, Boolean> = states
	fun clearStates() {
		states.clear()
	}

	fun getMotionAnimThreshold(): Float = 0.015f
}
