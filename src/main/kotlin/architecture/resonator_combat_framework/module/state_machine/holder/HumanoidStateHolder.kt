package architecture.resonator_combat_framework.module.state_machine.holder

import architecture.resonator_combat_framework.module.combat.ActionController
import net.minecraft.world.entity.Mob

/**
 * 人形生物状态持有者 —— [MobStateHolder] 的中间抽象层，
 * 预留给人形生物特有的状态扩展。
 *
 * @param T 实体类型，限定为 [Mob] 的子类
 * @param entity 所属生物
 * @param actionController 动作控制器
 */
abstract class HumanoidStateHolder<T : Mob>(
	entity: T,
	actionController: ActionController = ActionController(entity)
) : MobStateHolder<T>(entity, actionController)
