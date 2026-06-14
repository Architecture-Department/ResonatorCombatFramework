package architecture.resonator_combat_framework.module.entity_animation.animation.molang

import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Leashable
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import java.util.function.DoubleSupplier
import kotlin.math.atan2

open class MolangData {

	/** 全局可读写变量层 */
	private val vars = HashMap<String, DoubleSupplier>()

	/** 由子类（MolangScope）提供的局部变量查找 */
	protected open fun resolveLocal(name: String): Double? = null

	/** 由子类（MolangScope）处理的局部变量赋值，返回 true 表示已由子类处理 */
	protected open fun assignLocal(name: String, value: Double): Boolean = false

	open fun resolve(name: String): Double {
		resolveLocal(name)?.let { return it }
		return vars[name]?.asDouble ?: 0.0
	}

	open fun assign(name: String, value: Double) {
		if (assignLocal(name, value)) return
		if (name.startsWith("query.") ||
			name.startsWith("context.") ||
			name.startsWith("geometry.") ||
			name.startsWith("material.") ||
			name.startsWith("texture.")
		) return
		vars[name] = DoubleSupplier { value }
	}

	open fun assign(name: String, value: DoubleSupplier) {
		assign(name, value.asDouble)
	}

	fun get(name: String): Double = resolve(name)
	fun set(name: String, value: Double) = assign(name, value)
	fun set(name: String, value: DoubleSupplier) = assign(name, value)

	fun mergeInto(target: MutableMap<String, DoubleSupplier>) {
		target.putAll(vars)
	}

	fun updateAnimQueries(animTime: Float, deltaTime: Float) {
		vars[ANIM_TIME] = DoubleSupplier { animTime.toDouble() }
		vars[DELTA_TIME] = DoubleSupplier { deltaTime.toDouble() }
	}

	companion object {
		/** 当前动画已播放时间（秒） */
		const val ANIM_TIME: String = "query.anim_time"

		/** 实体总存活时间（秒） */
		const val LIFE_TIME: String = "query.life_time"

		/** 上一帧到当前帧的时间差（秒） */
		const val DELTA_TIME: String = "query.delta_time"

		/** 参与求值的实体数量 */
		const val ACTOR_COUNT: String = "query.actor_count"

		/** 是否处于格挡状态（1=是，0=否） */
		const val BLOCKING: String = "query.blocking"

		/** 站立位置的方块状态值 */
		const val BLOCK_STATE: String = "query.block_state"

		/** 身体俯仰角（度） */
		const val BODY_X_ROTATION: String = "query.body_x_rotation"

		/** 身体偏航角（度） */
		const val BODY_Y_ROTATION: String = "query.body_y_rotation"

		/** 能否攀爬（1=可，0=否） */
		const val CAN_CLIMB: String = "query.can_climb"

		/** 能否飞行（1=可，0=否） */
		const val CAN_FLY: String = "query.can_fly"

		/** 能否游泳（1=可，0=否） */
		const val CAN_SWIM: String = "query.can_swim"

		/** 能否行走（1=可，0=否） */
		const val CAN_WALK: String = "query.can_walk"

		/** 朝向主方向（0=南，1=西，2=北，3=东） */
		const val CARDINAL_FACING: String = "query.cardinal_facing"

		/** 二维朝向（忽略俯仰，0=南，1=西，2=北，3=东） */
		const val CARDINAL_FACING_2D: String = "query.cardinal_facing_2d"

		/** 玩家朝向主方向（0=南，1=西，2=北，3=东） */
		const val CARDINAL_PLAYER_FACING: String = "query.cardinal_player_facing"

		/** 动画控制器的速度倍率 */
		const val CONTROLLER_SPEED: String = "query.controller_speed"

		/** 当前游戏天数 */
		const val DAY: String = "query.day"

		/** 死亡后的 tick 数 */
		const val DEATH_TICKS: String = "query.death_ticks"

		/** 到摄像机的距离（格） */
		const val DISTANCE_FROM_CAMERA: String = "query.distance_from_camera"

		/** 装备栏物品总数 */
		const val EQUIPMENT_COUNT: String = "query.equipment_count"

		/** 渲染帧插值因子 [0,1) */
		const val FRAME_ALPHA: String = "query.frame_alpha"

		/** Actor 唯一标识 ID */
		const val GET_ACTOR_INFO_ID: String = "query.get_actor_info_id"

		/** 水平移动速度（格/秒） */
		const val GROUND_SPEED: String = "query.ground_speed"

		/** 是否装备披风（1=是，0=否） */
		const val HAS_CAPE: String = "query.has_cape"

		/** 是否有碰撞箱（1=有，0=无） */
		const val HAS_COLLISION: String = "query.has_collision"

		/** 是否受重力影响（1=是，0=否） */
		const val HAS_GRAVITY: String = "query.has_gravity"

		/** 是否戴头盔（1=是，0=否） */
		const val HAS_HEAD_GEAR: String = "query.has_head_gear"

		/** 是否有主人（1=有，0=无） */
		const val HAS_OWNER: String = "query.has_owner"

		/** 是否有玩家骑手（1=有，0=无） */
		const val HAS_PLAYER_RIDER: String = "query.has_player_rider"

		/** 是否有骑手（1=有，0=无） */
		const val HAS_RIDER: String = "query.has_rider"

		/** 头部俯仰角（度） */
		const val HEAD_X_ROTATION: String = "query.head_x_rotation"

		/** 头部偏航角（度） */
		const val HEAD_Y_ROTATION: String = "query.head_y_rotation"

		/** 当前生命值 */
		const val HEALTH: String = "query.health"

		/** 受击闪烁剩余 tick */
		const val HURT_TIME: String = "query.hurt_time"

		/** 受伤无敌剩余 tick */
		const val INVULNERABLE_TICKS: String = "query.invulnerable_ticks"

		/** 是否存活（1=存活，0=死亡） */
		const val IS_ALIVE: String = "query.is_alive"

		/** 是否愤怒（1=愤怒，0=平静） */
		const val IS_ANGRY: String = "query.is_angry"

		/** 是否幼年（1=幼年，0=成年） */
		const val IS_BABY: String = "query.is_baby"

		/** 是否在呼吸（1=呼吸，0=窒息） */
		const val IS_BREATHING: String = "query.is_breathing"

		/** 是否附魔（1=已附魔，0=未附魔） */
		const val IS_ENCHANTED: String = "query.is_enchanted"

		/** 是否火焰免疫（1=免疫，0=不免疫） */
		const val IS_FIRE_IMMUNE: String = "query.is_fire_immune"

		/** 是否第一人称视角（1=第一人称，0=第三人称） */
		const val IS_FIRST_PERSON: String = "query.is_first_person"

		/** 是否隐身（1=隐身，0=可见） */
		const val IS_INVISIBLE: String = "query.is_invisible"

		/** 是否接触水体（1=接触，0=未接触） */
		const val IS_IN_CONTACT_WITH_WATER: String = "query.is_in_contact_with_water"

		/** 是否在熔岩中（1=在，0=不在） */
		const val IS_IN_LAVA: String = "query.is_in_lava"

		/** 是否在水中（1=在，0=不在） */
		const val IS_IN_WATER: String = "query.is_in_water"

		/** 是否在水中或雨中（1=是，0=否） */
		const val IS_IN_WATER_OR_RAIN: String = "query.is_in_water_or_rain"

		/** 是否被拴绳（1=已拴，0=未拴） */
		const val IS_LEASHED: String = "query.is_leashed"

		/** 是否在移动（1=移动，0=静止） */
		const val IS_MOVING: String = "query.is_moving"

		/** 是否着火（1=着火，0=未着） */
		const val IS_ON_FIRE: String = "query.is_on_fire"

		/** 是否在地面（1=地面，0=空中/水中） */
		const val IS_ON_GROUND: String = "query.is_on_ground"

		/** 是否被红石激活（1=激活，0=未激活） */
		const val IS_POWERED: String = "query.is_powered"

		/** 是否在骑乘（1=在骑，0=未骑） */
		const val IS_RIDING: String = "query.is_riding"

		/** 是否被装鞍（1=有鞍，0=无鞍） */
		const val IS_SADDLED: String = "query.is_saddled"

		/** 是否静音（1=静音，0=正常） */
		const val IS_SILENT: String = "query.is_silent"

		/** 是否在睡觉（1=在睡，0=未睡） */
		const val IS_SLEEPING: String = "query.is_sleeping"

		/** 是否在潜行（1=潜行，0=站立） */
		const val IS_SNEAKING: String = "query.is_sneaking"

		/** 是否在疾跑（1=疾跑，0=行走） */
		const val IS_SPRINTING: String = "query.is_sprinting"

		/** 物品能否堆叠（1=可，0=不可） */
		const val IS_STACKABLE: String = "query.is_stackable"

		/** 是否在游泳（1=游泳，0=非游泳） */
		const val IS_SWIMMING: String = "query.is_swimming"

		/** 是否在使用物品（1=使用中，0=未用） */
		const val IS_USING_ITEM: String = "query.is_using_item"

		/** 是否在爬墙（1=攀爬，0=未攀爬） */
		const val IS_WALL_CLIMBING: String = "query.is_wall_climbing"

		/** 物品最大可使用时长（tick） */
		const val ITEM_MAX_USE_DURATION: String = "query.item_max_use_duration"

		/** 主手物品最大使用时长（tick） */
		const val MAIN_HAND_ITEM_MAX_DURATION: String = "query.main_hand_item_max_duration"

		/** 主手物品已用时长（tick） */
		const val MAIN_HAND_ITEM_USE_DURATION: String = "query.main_hand_item_use_duration"

		/** 物品最大耐久度 */
		const val MAX_DURABILITY: String = "query.max_durability"

		/** 最大生命值 */
		const val MAX_HEALTH: String = "query.max_health"

		/** 月亮亮度（0=新月，1=满月） */
		const val MOON_BRIGHTNESS: String = "query.moon_brightness"

		/** 月相（0=满月~7=盈凸月） */
		const val MOON_PHASE: String = "query.moon_phase"

		/** 移动方向角度（度，0=南，90=西） */
		const val MOVEMENT_DIRECTION: String = "query.movement_direction"

		/** 玩家经验等级 */
		const val PLAYER_LEVEL: String = "query.player_level"

		/** 物品剩余耐久度 */
		const val REMAINING_DURABILITY: String = "query.remaining_durability"

		@JvmStatic
		fun of(holder: Any?): MolangData {
			if (holder is Entity) {
				return holder.getData(RcfAttachmentTypes.MOLANG_DATA)
			}
			if (holder is Level) {
				return holder.getData(RcfAttachmentTypes.MOLANG_DATA)
			}
			throw IllegalArgumentException("MolangData can only be attached to Level or Entity. Unsupported holder type: ${holder?.javaClass}")
		}

		@JvmStatic
		fun MolangData.initEntityQueries(entity: Entity) {
			vars[LIFE_TIME] = DoubleSupplier { (entity.tickCount / 20f).toDouble() }
			vars[IS_ALIVE] = DoubleSupplier { if (entity.isAlive) 1.0 else 0.0 }
			vars[IS_SNEAKING] = DoubleSupplier { if (entity.isShiftKeyDown) 1.0 else 0.0 }
			vars[IS_SPRINTING] = DoubleSupplier { if (entity.isSprinting) 1.0 else 0.0 }
			vars[IS_ON_GROUND] = DoubleSupplier { if (entity.onGround()) 1.0 else 0.0 }
			vars[IS_SWIMMING] = DoubleSupplier { if (entity.isSwimming) 1.0 else 0.0 }
			vars[IS_MOVING] = DoubleSupplier {
				if (entity.horizontalCollision || entity.verticalCollision) 0.0
				else if (entity.deltaMovement.horizontalDistanceSqr() > 0.001) 1.0 else 0.0
			}
			vars[GROUND_SPEED] = DoubleSupplier { entity.deltaMovement.horizontalDistance() }
			vars[MOVEMENT_DIRECTION] = DoubleSupplier {
				val dx = -entity.deltaMovement.x
				val dz = entity.deltaMovement.z
				Math.toDegrees(atan2(dx, dz)).let { if (it < 0) it + 360.0 else it }
			}
			vars[HEAD_X_ROTATION] = DoubleSupplier { entity.xRot.toDouble() }
			vars[HEAD_Y_ROTATION] = DoubleSupplier { entity.yRot.toDouble() }
			vars[BODY_X_ROTATION] = DoubleSupplier { entity.xRotO.toDouble() }
			vars[INVULNERABLE_TICKS] = DoubleSupplier { entity.invulnerableTime.toDouble() }
			vars[IS_IN_WATER] = DoubleSupplier { if (entity.isInWater) 1.0 else 0.0 }
			vars[IS_IN_WATER_OR_RAIN] = DoubleSupplier { if (entity.isInWaterOrRain) 1.0 else 0.0 }
			vars[IS_IN_LAVA] = DoubleSupplier { if (entity.isInLava) 1.0 else 0.0 }
			vars[IS_ON_FIRE] = DoubleSupplier { if (entity.isOnFire) 1.0 else 0.0 }
			vars[HAS_GRAVITY] = DoubleSupplier { if (entity.isNoGravity) 0.0 else 1.0 }
			vars[IS_BREATHING] = DoubleSupplier { if (entity.airSupply > 0) 1.0 else 0.0 }
			vars[IS_INVISIBLE] = DoubleSupplier { if (entity.isInvisible) 1.0 else 0.0 }
			vars[IS_RIDING] = DoubleSupplier { if (entity.isPassenger) 1.0 else 0.0 }
			vars[HAS_RIDER] = DoubleSupplier { if (entity.isVehicle) 1.0 else 0.0 }
			vars[DAY] = DoubleSupplier { (entity.level().dayTime() / 24000L).toDouble() }
			vars[MOON_PHASE] = DoubleSupplier { entity.level().moonPhase.toDouble() }
			vars[MOON_BRIGHTNESS] = DoubleSupplier { entity.level().moonBrightness.toDouble() }
			vars[CARDINAL_FACING] =
				DoubleSupplier { ((entity.yRot + 180.0) / 90.0).toInt().let { ((it % 4 + 4) % 4).toDouble() } }

			if (entity is LivingEntity) {
				vars[BODY_Y_ROTATION] = DoubleSupplier { entity.yBodyRot.toDouble() }
				vars[HURT_TIME] = DoubleSupplier { entity.hurtTime.toDouble() }
				vars[BLOCKING] = DoubleSupplier { if (entity.isUsingItem) 1.0 else 0.0 }
				vars[IS_USING_ITEM] = DoubleSupplier { if (entity.isUsingItem) 1.0 else 0.0 }
				vars[ITEM_MAX_USE_DURATION] = DoubleSupplier { entity.useItem.getUseDuration(entity).toDouble() }
				vars[HEALTH] = DoubleSupplier { entity.health.toDouble() }
				vars[MAX_HEALTH] = DoubleSupplier { entity.maxHealth.toDouble() }
				vars[IS_BABY] = DoubleSupplier { if (entity.isBaby) 1.0 else 0.0 }
				vars[DEATH_TICKS] = DoubleSupplier { entity.deathTime.toDouble() }
			}

			if (entity is Leashable) {
				vars[IS_LEASHED] = DoubleSupplier { if (entity.isLeashed) 1.0 else 0.0 }
			}

			if (entity is Player) {
				vars[PLAYER_LEVEL] = DoubleSupplier { entity.experienceLevel.toDouble() }
				vars[IS_SLEEPING] = DoubleSupplier { if (entity.isSleeping) 1.0 else 0.0 }
			}

			if (entity.level().isClientSide) {
				vars[IS_FIRST_PERSON] = DoubleSupplier {
					val opts = Minecraft.getInstance().options ?: return@DoubleSupplier 0.0
					if (opts.cameraType.isFirstPerson) 1.0 else 0.0
				}
			}
		}
	}
}
