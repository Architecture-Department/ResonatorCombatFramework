package architecture.resonator_combat_framework.animation

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import kotlin.math.max
import kotlin.math.sqrt

class MovementAnimationDef
@JvmOverloads
constructor(
	id: ResourceLocation,
	animationId: ResourceLocation,
	stateModifiers: Map<ResourceLocation, Boolean> = emptyMap(),
	/**
	 * 预期最大水平速度（方块/刻）。
	 * 实体当前速度达到此值时 animSpeedRatio = 1.0。
	 */
	var expectedMaxSpeed: Float = 0.25f,
) : ActionAnimationDef(id, animationId, stateModifiers) {

	constructor(
		id: ResourceLocation,
		stateModifiers: Map<ResourceLocation, Boolean> = emptyMap(),
		expectedMaxSpeed: Float = 0.25f
	) : this(id, id, stateModifiers, expectedMaxSpeed)

	fun getAnimSpeedRatio(entity: Entity): Float {
		val hSpeed = sqrt(
			entity.deltaMovement.x * entity.deltaMovement.x +
				entity.deltaMovement.z * entity.deltaMovement.z
		).toFloat()
		return max(hSpeed / expectedMaxSpeed, 0f)
	}
}
