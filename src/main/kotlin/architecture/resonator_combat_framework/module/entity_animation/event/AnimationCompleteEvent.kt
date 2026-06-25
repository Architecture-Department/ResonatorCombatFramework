package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import net.neoforged.bus.api.Event

/**
 * 动画完成事件 —— 动画自然播放完毕时触发（非手动停止）。
 */
@AllOpe
class AnimationCompleteEvent(
	val controller: IEntityAnimationController<*>,
) : Event()
