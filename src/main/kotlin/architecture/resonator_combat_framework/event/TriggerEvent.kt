package architecture.resonator_combat_framework.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.animation.AnimationDef
import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.data.PlayConfig
import net.neoforged.bus.api.Event

/**
 * 动画触发事件 —— 当 [IEntityAnimationController.trigger] 或 [IEntityAnimationController.triggerWithAnimation] 被调用时触发。
 * 包含 [Pre]（触发前，可取消）和 [Post]（触发后）两个子事件，
 * 监听此事件可拦截或响应动画触发。
 */
@AllOpe
class TriggerEvent(
	val controller: IEntityAnimationController<*>,
	val anim: AnimationDef,
	val config: PlayConfig,
) : Event() {

	/**
	 * 动画触发前事件 —— 在动画实际触发前发出。
	 * 可取消，取消后将阻止本次动画触发。
	 */
	class Pre(
		controller: IEntityAnimationController<*>,
		anim: AnimationDef,
		config: PlayConfig,
	) : TriggerEvent(controller, anim, config)

	/**
	 * 动画触发后事件 —— 在动画已触发后发出，包含最终的动画定义和播放配置。
	 */
	class Post(
		controller: IEntityAnimationController<*>,
		anim: AnimationDef,
		config: PlayConfig,
	) : TriggerEvent(controller, anim, config)
}
