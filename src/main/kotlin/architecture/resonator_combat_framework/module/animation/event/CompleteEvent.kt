package architecture.resonator_combat_framework.module.animation.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.animation.controller.IEntityAnimationController
import net.neoforged.bus.api.Event

/**
 * 动画完成事件 —— 动画自然播放完毕时触发（非手动停止）。
 */
@AllOpe
class CompleteEvent(
	val controller: IEntityAnimationController<*>,
) : Event()
