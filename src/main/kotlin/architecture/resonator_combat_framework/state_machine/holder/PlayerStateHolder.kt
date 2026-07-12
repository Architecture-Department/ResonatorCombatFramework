package architecture.resonator_combat_framework.state_machine.holder

import architecture.resonator_combat_framework.combat.ActionController
import net.minecraft.world.entity.player.Player

/**
 * 玩家状态持有者 —— 为 [Player] 提供状态管理能力。
 * 直接继承 [EntityStateHolder]，无额外扩展。
 *
 * @param entity 所属玩家
 * @param actionController 动作控制器
 */
class PlayerStateHolder(
	entity: Player,
	actionController: ActionController = ActionController(entity)
) : EntityStateHolder<Player>(entity, actionController)
