package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.module.entity_animation.animation.model.BrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import kotlin.math.max
import kotlin.math.sqrt

class MovementAnimation
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
) : ActionAnimation(id, animationId, stateModifiers) {

	constructor(
		id: ResourceLocation,
		stateModifiers: Map<ResourceLocation, Boolean> = emptyMap(),
		expectedMaxSpeed: Float = 0.25f
	) : this(id, id, stateModifiers, expectedMaxSpeed)

	/**
	 * 当前动画速度倍率
	 * = 实体实际水平速度 / expectedMaxSpeed，再钳制到 [0, ) 范围。
	 * 控制器可读取此值来调整动画播放速度。
	 */
	@Volatile
	var animSpeedRatio: Float = 0f
		protected set

	override fun tick(
		entity: Entity,
		animTime: Float,
		deltaTime: Float,
		proxyModel: ProxyModel,
		brModel: BrModel
	) {
		super.tick(entity, animTime, deltaTime, proxyModel, brModel)
		val hSpeed = sqrt(
			entity.deltaMovement.x * entity.deltaMovement.x +
				entity.deltaMovement.z * entity.deltaMovement.z
		).toFloat()
		animSpeedRatio = max(hSpeed / expectedMaxSpeed, 0f)
	}

	override fun onEnd(entity: Entity) {
		super.onEnd(entity)
		animSpeedRatio = 0f
	}
}
