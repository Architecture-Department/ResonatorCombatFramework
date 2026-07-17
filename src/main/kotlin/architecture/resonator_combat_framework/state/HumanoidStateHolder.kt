package architecture.resonator_combat_framework.state

import net.minecraft.world.entity.Mob

abstract class HumanoidStateHolder<T : Mob>(
	entity: T,
) : MobStateHolder<T>(entity)
