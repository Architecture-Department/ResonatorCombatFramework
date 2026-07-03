package architecture.resonator_combat_framework.module.animation.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.combat.AttackActionPhase
import net.neoforged.bus.api.Event

/**
 * 攻击阶段事件 —— 攻击动画的阶段（如蓄力、挥砍、收招）开始/结束时触发。
 */
@AllOpe
class PhaseEvent(
	val controller: IEntityAnimationController<*>,
	val phase: AttackActionPhase,
) : Event() {

    /**
     * 阶段开始事件 —— 当攻击动画进入某个 [AttackActionPhase] 时触发。
     */
    class Start(
	    controller: IEntityAnimationController<*>,
	    phase: AttackActionPhase,
    ) : PhaseEvent(controller, phase)

    /**
     * 阶段结束事件 —— 当攻击动画离开某个 [AttackActionPhase] 时触发。
     */
    class End(
	    controller: IEntityAnimationController<*>,
	    phase: AttackActionPhase,
    ) : PhaseEvent(controller, phase)
}
