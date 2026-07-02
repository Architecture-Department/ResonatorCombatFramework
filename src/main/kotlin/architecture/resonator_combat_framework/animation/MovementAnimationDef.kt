package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationDef
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 移动动画定义。
 *
 * 扩展 [AnimationDef]，用于描述实体的移动类动画，
 * 并根据实体的当前水平速度与预期最大速度的比例提供动画播放速度缩放因子。
 *
 * @property expectedMaxSpeed 预期最大水平速度（方块/刻），实体速度达到此值时动画播放速度为 1.0
 */
class MovementAnimationDef
@JvmOverloads
constructor(
	id: ResourceLocation,
	animationId: ResourceLocation,
	/**
	 * 预期最大水平速度（方块/刻）。
	 * 实体当前速度达到此值时 animSpeedRatio = 1.0。
	 */
	var expectedMaxSpeed: Float = 0.25f,
) : AnimationDef(id, animationId) {

	/**
	 * 使用与动画定义 ID 相同的 ID 同时作为动画资源 ID 的便捷构造方法。
	 *
	 * @param id 动画定义 ID 与动画资源 ID
	 * @param expectedMaxSpeed 预期最大水平速度
	 */
	constructor(
		id: ResourceLocation,
		expectedMaxSpeed: Float = 0.25f
	) : this(id, id, expectedMaxSpeed)

	/**
	 * 计算动画播放速度比例。
	 *
	 * 基于实体当前水平速度与 [expectedMaxSpeed] 的比值，
	 * 返回值在 0.0 ~ 1.0 之间，用于驱动动画混合树中的速度参数，
	 * 实现走跑动画的自然过渡。
	 *
	 * @param entity 目标实体
	 * @return 动画速度比例（0.0 ~ 1.0）
	 */
	fun getAnimSpeedRatio(entity: Entity): Float {
		val hSpeed = sqrt(
			entity.deltaMovement.x * entity.deltaMovement.x +
				entity.deltaMovement.z * entity.deltaMovement.z
		).toFloat()
		return max(hSpeed / expectedMaxSpeed, 0f)
	}
}
