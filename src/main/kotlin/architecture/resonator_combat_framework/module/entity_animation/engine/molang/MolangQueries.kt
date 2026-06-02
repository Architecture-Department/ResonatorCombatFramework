package architecture.resonator_combat_framework.module.entity_animation.engine.molang


import architecture.resonator_combat_framework.module.entity_animation.engine.molang.value.Variable
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.entity.BlockEntity
import java.util.function.DoubleSupplier

object MolangQueries {
	// Query name constants
	const val ANIM_TIME: String = "query.anim_time"
	const val LIFE_TIME: String = "query.life_time"
	const val ACTOR_COUNT: String = "query.actor_count"
	const val BLOCKING: String = "query.blocking"
	const val BLOCK_STATE: String = "query.block_state"
	const val BODY_X_ROTATION: String = "query.body_x_rotation"
	const val BODY_Y_ROTATION: String = "query.body_y_rotation"
	const val CAN_CLIMB: String = "query.can_climb"
	const val CAN_FLY: String = "query.can_fly"
	const val CAN_SWIM: String = "query.can_swim"
	const val CAN_WALK: String = "query.can_walk"
	const val CARDINAL_FACING: String = "query.cardinal_facing"
	const val CARDINAL_FACING_2D: String = "query.cardinal_facing_2d"
	const val CARDINAL_PLAYER_FACING: String = "query.cardinal_player_facing"
	const val CONTROLLER_SPEED: String = "query.controller_speed"
	const val DAY: String = "query.day"
	const val DEATH_TICKS: String = "query.death_ticks"
	const val DISTANCE_FROM_CAMERA: String = "query.distance_from_camera"
	const val EQUIPMENT_COUNT: String = "query.equipment_count"
	const val FRAME_ALPHA: String = "query.frame_alpha"
	const val GET_ACTOR_INFO_ID: String = "query.get_actor_info_id"
	const val GROUND_SPEED: String = "query.ground_speed"
	const val HAS_CAPE: String = "query.has_cape"
	const val HAS_COLLISION: String = "query.has_collision"
	const val HAS_GRAVITY: String = "query.has_gravity"
	const val HAS_HEAD_GEAR: String = "query.has_head_gear"
	const val HAS_OWNER: String = "query.has_owner"
	const val HAS_PLAYER_RIDER: String = "query.has_player_rider"
	const val HAS_RIDER: String = "query.has_rider"
	const val HEAD_X_ROTATION: String = "query.head_x_rotation"
	const val HEAD_Y_ROTATION: String = "query.head_y_rotation"
	const val HEALTH: String = "query.health"
	const val HURT_TIME: String = "query.hurt_time"
	const val INVULNERABLE_TICKS: String = "query.invulnerable_ticks"
	const val IS_ALIVE: String = "query.is_alive"
	const val IS_ANGRY: String = "query.is_angry"
	const val IS_BABY: String = "query.is_baby"
	const val IS_BREATHING: String = "query.is_breathing"
	const val IS_ENCHANTED: String = "query.is_enchanted"
	const val IS_FIRE_IMMUNE: String = "query.is_fire_immune"
	const val IS_FIRST_PERSON: String = "query.is_first_person"
	const val IS_INVISIBLE: String = "query.is_invisible"
	const val IS_IN_CONTACT_WITH_WATER: String = "query.is_in_contact_with_water"
	const val IS_IN_LAVA: String = "query.is_in_lava"
	const val IS_IN_WATER: String = "query.is_in_water"
	const val IS_IN_WATER_OR_RAIN: String = "query.is_in_water_or_rain"
	const val IS_LEASHED: String = "query.is_leashed"
	const val IS_MOVING: String = "query.is_moving"
	const val IS_ON_FIRE: String = "query.is_on_fire"
	const val IS_ON_GROUND: String = "query.is_on_ground"
	const val IS_POWERED: String = "query.is_powered"
	const val IS_RIDING: String = "query.is_riding"
	const val IS_SADDLED: String = "query.is_saddled"
	const val IS_SILENT: String = "query.is_silent"
	const val IS_SLEEPING: String = "query.is_sleeping"
	const val IS_SNEAKING: String = "query.is_sneaking"
	const val IS_SPRINTING: String = "query.is_sprinting"
	const val IS_STACKABLE: String = "query.is_stackable"
	const val IS_SWIMMING: String = "query.is_swimming"
	const val IS_USING_ITEM: String = "query.is_using_item"
	const val IS_WALL_CLIMBING: String = "query.is_wall_climbing"
	const val ITEM_MAX_USE_DURATION: String = "query.item_max_use_duration"
	const val MAIN_HAND_ITEM_MAX_DURATION: String = "query.main_hand_item_max_duration"
	const val MAIN_HAND_ITEM_USE_DURATION: String = "query.main_hand_item_use_duration"
	const val MAX_DURABILITY: String = "query.max_durability"
	const val MAX_HEALTH: String = "query.max_health"
	const val MOON_BRIGHTNESS: String = "query.moon_brightness"
	const val MOON_PHASE: String = "query.moon_phase"
	const val MOVEMENT_DIRECTION: String = "query.movement_direction"
	const val PLAYER_LEVEL: String = "query.player_level"
	const val REMAINING_DURABILITY: String = "query.remaining_durability"
	private val VARIABLE_MAP = HashMap<String, Variable>()

	@Synchronized
	fun registerVariable(name: String): Variable {
		return VARIABLE_MAP.computeIfAbsent(name) { k: String -> Variable(k, 0.0) }
	}

	@Synchronized
	fun getVariableFor(name: String): Variable {
		return VARIABLE_MAP.computeIfAbsent(name) { k: String -> Variable(k, 0.0) }
	}

	fun setVariable(name: String, supplier: DoubleSupplier) {
		getVariableFor(name).set(supplier)
	}

	// Called each tick to update query values
	fun setDefaultQueryValues(actor: Actor) {
		setVariable(ANIM_TIME) { actor.animTime.toDouble() }
		setVariable(LIFE_TIME) { actor.lifeTime.toDouble() }
		setVariable(FRAME_ALPHA) { actor.frameAlpha.toDouble() }
		setVariable(CONTROLLER_SPEED) { actor.controllerSpeed.toDouble() }
		setVariable(ACTOR_COUNT) { 1.0 }
	}

	fun setDefaultEntityQueryValues(actor: Actor) {
		setDefaultQueryValues(actor)
		if (actor.entity == null) return
		val e = actor.entity!!
		setVariable(IS_SNEAKING) { if (e.isShiftKeyDown) 1.0 else 0.0 }
		setVariable(IS_SPRINTING) { if (e.isSprinting) 1.0 else 0.0 }
		setVariable(IS_ON_GROUND) { if (e.onGround()) 1.0 else 0.0 }
		setVariable(IS_SWIMMING) { if (e.isSwimming) 1.0 else 0.0 }
		setVariable(IS_IN_WATER) { if (e.isInWater) 1.0 else 0.0 }
		setVariable(HEALTH) { e.health.toDouble() }
		setVariable(MAX_HEALTH) { e.maxHealth.toDouble() }
		setVariable(IS_ALIVE) { if (e.isAlive) 1.0 else 0.0 }
		setVariable(IS_BABY) { if (e.isBaby) 1.0 else 0.0 }
		setVariable(
			IS_MOVING
		) {
			if (e.horizontalCollision || e.verticalCollision) 0.0 else (if (e.deltaMovement
					.horizontalDistanceSqr() > 0.001
			) 1.0 else 0.0)
		}
		setVariable(GROUND_SPEED) { e.deltaMovement.horizontalDistance() }
		setVariable(HAS_GRAVITY) { if (e.isNoGravity) 0.0 else 1.0 }
		// More entity-specific queries...
	}

	fun setDefaultBlockEntityQueryValues(actor: Actor) {
		setDefaultQueryValues(actor)
		if (actor.blockEntity != null) {
			// Block entity specific queries
		}
	}

	class Actor {
		var entity: LivingEntity? = null
		var blockEntity: BlockEntity? = null
		var animTime: Float = 0f
		var lifeTime: Float = 0f
		var frameAlpha: Float = 0f
		var controllerSpeed: Float = 0f

		constructor()

		constructor(entity: LivingEntity?) {
			this.entity = entity
		}

		constructor(blockEntity: BlockEntity?) {
			this.blockEntity = blockEntity
		}
	}
}

