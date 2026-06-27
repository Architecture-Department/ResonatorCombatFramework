package architecture.resonator_combat_framework.module.entity_state_machine.holder

import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionController
import net.minecraft.world.entity.Mob

class MobStateHolder<T : Mob>(
	entity: T,
	actionController: ActionController = ActionController(entity)
) : EntityStateHolder<T>(entity, actionController)
