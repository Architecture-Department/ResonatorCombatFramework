package architecture.resonator_combat_framework.state

import net.minecraft.world.entity.Mob

class MobStateHolder<T : Mob>(
	entity: T,
) : EntityStateHolder<T>(entity)
