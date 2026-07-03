package architecture.resonator_combat_framework.module.state_machine.holder

import architecture.resonator_combat_framework.module.combat.ActionController
import net.minecraft.world.entity.Mob

/**
 * 生物状态持有者 —— 为 [Mob] 提供状态管理能力。
 * 直接继承 [EntityStateHolder]，无额外扩展。
 *
 * @param T 实体类型，限定为 [Mob] 的子类
 * @param entity 所属生物
 * @param actionController 动作控制器
 */
class MobStateHolder<T : Mob>(
	entity: T,
	actionController: ActionController = ActionController(entity)
) : EntityStateHolder<T>(entity, actionController)
