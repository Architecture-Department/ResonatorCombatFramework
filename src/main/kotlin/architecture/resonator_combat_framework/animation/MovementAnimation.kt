package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import kotlin.math.sqrt

/**
 * 移动动画 —— 读取实体速度以控制动画播放速度的 [ActionAnimation]。
 *
 * 适用于行走、奔跑、潜行、游泳等移动类动画。
 *
 * 相比 [ActionAnimation]，增加了：
 * 1. [expectedMaxSpeed]：预期最大水平速度，用于归一化动画速度倍率
 * 2. [animSpeedRatio]：当前动画速度倍率（只读），基于实体实际水平速度计算
 *
 * 注意：MovementAnimation 不修改实体的任何属性（速度、位置、方向等），
 * 仅读取实体速度来计算动画播放倍率。实际的移动逻辑由原版或其他系统处理。
 */
class MovementAnimation(
	id: ResourceLocation,
	animationId: String,
	stateModifiers: Map<ResourceLocation, Boolean> = emptyMap(),
	/**
	 * 预期最大水平速度（方块/刻）。
	 * 实体当前速度达到此值时 animSpeedRatio = 1.0。
	 */
	val expectedMaxSpeed: Float = 0.25f,
) : ActionAnimation(id, animationId, stateModifiers) {

	constructor(
		id: ResourceLocation,
		stateModifiers: Map<ResourceLocation, Boolean> = emptyMap(),
		expectedMaxSpeed: Float = 0.25f
	) : this(id, id.namespace + "." + id.path, stateModifiers, expectedMaxSpeed)

	constructor(
		animationId: String,
		stateModifiers: Map<ResourceLocation, Boolean> = emptyMap(),
		expectedMaxSpeed: Float = 0.25f
	) : this(RcfUtil.modRl(animationId), animationId, stateModifiers, expectedMaxSpeed)

	/**
	 * 当前动画速度倍率（0 ~ 2.0）。
	 * = 实体实际水平速度 / expectedMaxSpeed，再钳制到 [0, 2] 范围。
	 * 控制器可读取此值来调整动画播放速度。
	 */
	@Volatile
	var animSpeedRatio: Float = 0f
		protected set

	override fun onTick(entity: LivingEntity, animTime: Float, deltaTime: Float) {
		super.onTick(entity, animTime, deltaTime)
		val hSpeed = sqrt(
			entity.deltaMovement.x * entity.deltaMovement.x +
				entity.deltaMovement.z * entity.deltaMovement.z
		).toFloat()
		animSpeedRatio = (hSpeed / expectedMaxSpeed).coerceIn(0f, 2f)
	}

	override fun onEnd(entity: LivingEntity) {
		super.onEnd(entity)
		animSpeedRatio = 0f
	}
}