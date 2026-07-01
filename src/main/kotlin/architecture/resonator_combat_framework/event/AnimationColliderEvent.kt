package architecture.resonator_combat_framework.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event

/**
 * 碰撞体更新事件 —— [AttackAnimation.tickAdvance] 中每 tick 更新碰撞体时触发。
 *
 * 监听此事件可动态修改碰撞体的属性（大小、位置、效果等）。
 */
@AllOpe
sealed class AnimationColliderEvent(
	val controller: IEntityAnimationController<*>,
	val entity: Entity,
	val animTime: Float,
	val poseData: PoseData,
	val brModel: BrModel,
	val mergedProxy: PoseData,
) : Event() {

	/** 碰撞体更新前触发 */
	class Pre(
		controller: IEntityAnimationController<*>,
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: BrModel,
		mergedProxy: PoseData,
	) : AnimationColliderEvent(controller, entity, animTime, poseData, brModel, mergedProxy)

	/** 碰撞体更新后触发 */
	class Post(
		controller: IEntityAnimationController<*>,
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: BrModel,
		mergedProxy: PoseData,
	) : AnimationColliderEvent(controller, entity, animTime, poseData, brModel, mergedProxy)
}