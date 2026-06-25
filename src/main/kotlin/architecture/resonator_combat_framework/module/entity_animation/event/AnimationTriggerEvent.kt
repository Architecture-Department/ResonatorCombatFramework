package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimationPlayData
import net.neoforged.bus.api.Event

/**
 * 动画触发事件 —— 当 [IEntityAnimationController.trigger] 或 [IEntityAnimationController.triggerWithAnimation] 被调用时触发。
 *
 * 监听此事件可拦截或响应动画触发。
 */
@AllOpe
class AnimationTriggerEvent(
	val controller: IEntityAnimationController<*>,
	val anim: StaticAnimation,
	val config: AnimationPlayData,
) : Event() {

	/** 触发前（可取消） */
	class Pre(
		controller: IEntityAnimationController<*>,
		anim: StaticAnimation,
		config: AnimationPlayData,
	) : AnimationTriggerEvent(controller, anim, config)

	/** 触发后 */
	class Post(
		controller: IEntityAnimationController<*>,
		anim: StaticAnimation,
		config: AnimationPlayData,
	) : AnimationTriggerEvent(controller, anim, config)
}
