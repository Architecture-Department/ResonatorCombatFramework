package architecture.resonator_combat_framework.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.animation.model.GeometryModel
import architecture.resonator_combat_framework.module.animation.model.PoseData
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event

/**
 * 碰撞体更新事件 —— [AttackAnimation.tickAdvance] 中每 tick 更新碰撞体时触发。
 *
 * 监听此事件可动态修改碰撞体的属性（大小、位置、效果等）。
 *
 * @property controller 当前动画控制器
 * @property entity    持有碰撞体的实体
 * @property animTime  当前动画时间（秒）
 * @property poseData  当前帧的姿态数据
 * @property brModel   几何模型引用
 * @property mergedProxy 合并后的代理姿态数据
 */
@AllOpe
sealed class ColliderEvent(
	val controller: IEntityAnimationController<*>,
	val entity: Entity,
	val animTime: Float,
	val poseData: PoseData,
	val brModel: GeometryModel,
	val mergedProxy: PoseData,
) : Event() {

	/**
	 * 碰撞体更新前触发。
	 *
	 * 在此事件中可修改碰撞体的初始属性（如位置、大小），
	 * 修改结果将作用于本次碰撞更新。
	 */
	class Pre(
		controller: IEntityAnimationController<*>,
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: GeometryModel,
		mergedProxy: PoseData,
	) : ColliderEvent(controller, entity, animTime, poseData, brModel, mergedProxy)

	/**
	 * 碰撞体更新后触发。
	 *
	 * 在此事件中可读取最终的碰撞体状态，
	 * 或进行后处理（如触发粒子特效、音效等）。
	 */
	class Post(
		controller: IEntityAnimationController<*>,
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: GeometryModel,
		mergedProxy: PoseData,
	) : ColliderEvent(controller, entity, animTime, poseData, brModel, mergedProxy)
}
