package architecture.resonator_combat_framework.module.entity_state_machine.holder

import net.minecraft.world.entity.Mob

abstract class HumanoidStateHolder<T : Mob>(entity: T) : MobStateHolder<T>(entity)
