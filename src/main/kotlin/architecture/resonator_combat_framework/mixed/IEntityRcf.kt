package architecture.resonator_combat_framework.mixed

import net.minecraft.world.entity.Entity

interface IEntityRcf {

	companion object {
		@JvmStatic
		fun of(entity: Entity?): IEntityRcf? {
			return entity
		}
	}
}
