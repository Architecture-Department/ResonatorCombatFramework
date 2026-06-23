package architecture.resonator_combat_framework.module.entity_state_machine.holder

import net.minecraft.world.entity.Mob

class MobStateHolder<T : Mob>(entity: T) : EntityStateHolder<T>(entity)
