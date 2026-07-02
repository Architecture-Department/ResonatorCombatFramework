package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_state_machine.combat.AttackPhase
import net.neoforged.bus.api.Event

/**
 * 攻击阶段事件 —— 攻击动画的阶段（如蓄力、挥砍、收招）开始/结束时触发。
 */
@AllOpe
class PhaseEvent(
    val controller: IEntityAnimationController<*>,
    val phase: AttackPhase,
) : Event() {

    /**
     * 阶段开始事件 —— 当攻击动画进入某个 [AttackPhase] 时触发。
     */
    class Start(
        controller: IEntityAnimationController<*>,
        phase: AttackPhase,
    ) : PhaseEvent(controller, phase)

    /**
     * 阶段结束事件 —— 当攻击动画离开某个 [AttackPhase] 时触发。
     */
    class End(
        controller: IEntityAnimationController<*>,
        phase: AttackPhase,
    ) : PhaseEvent(controller, phase)
}
