package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.animation.AttackPhase
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import net.neoforged.bus.api.Event

/**
 * 攻击阶段事件 —— 攻击动画的阶段开始/结束时触发。
 */
@AllOpe
class AnimationPhaseEvent(
	val controller: IEntityAnimationController<*>,
	val phase: AttackPhase,
) : Event() {

	/** 阶段开始 */
	class Start(
		controller: IEntityAnimationController<*>,
		phase: AttackPhase,
	) : AnimationPhaseEvent(controller, phase)

	/** 阶段结束 */
	class End(
		controller: IEntityAnimationController<*>,
		phase: AttackPhase,
	) : AnimationPhaseEvent(controller, phase)
}
