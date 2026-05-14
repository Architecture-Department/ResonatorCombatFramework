package architecture.resonator_combat_framework.mixed

import architecture.goldenboughs_lib.api.NoMixinException
import architecture.resonator_combat_framework.api.AppurtenanceHost
import architecture.resonator_combat_framework.api.appurtenance.AppurtenanceInfo
import net.minecraft.world.entity.Entity

interface IEntityRcf : AppurtenanceHost {
	fun `resonator_combat_framework$getAppurtenanceInfoMap`(): MutableMap<String, AppurtenanceInfo<*>> =
		throw NoMixinException()

	override val appurtenanceInfoMap: MutableMap<String, AppurtenanceInfo<*>>
		get() = `resonator_combat_framework$getAppurtenanceInfoMap`()

	companion object {
		@JvmStatic
		fun of(entity: Entity?): IEntityRcf? {
			return entity
		}
	}
}
