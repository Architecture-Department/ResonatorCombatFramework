package architecture.resonator_combat_framework.module.entity_state_machine.holder

import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionController
import net.minecraft.world.entity.player.Player

class PlayerStateHolder(
	entity: Player,
	actionController: ActionController = ActionController(entity)
) : EntityStateHolder<Player>(entity, actionController)
