package architecture.resonator_combat_framework.mixed

import net.minecraft.world.entity.Entity

/**
 * RCF 实体扩展接口 —— 通过 mixin 注入到所有 Entity 实例。
 * 提供类型安全的实体向下转型，用于其他 RCF 组件的访问。
 */
interface IEntityRcf {

	companion object {
		@JvmStatic
		fun of(entity: Entity?): IEntityRcf? {
			return entity
		}
	}
}
