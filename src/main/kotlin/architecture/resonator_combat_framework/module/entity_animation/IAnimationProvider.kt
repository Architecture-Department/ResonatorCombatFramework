package architecture.resonator_combat_framework.module.entity_animation

import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapperProvider

/**
 * 动画提供器接口——为实体提供动画控制器映射提供器。
 * 实现类负责将动画控制器与实际实体类型绑定。
 */
interface IAnimationProvider {
	fun `resonator_combat_framework$getMapperProvider`(): IEntityAnimationMapperProvider<*, *>

	companion object {
		fun IAnimationProvider.getMapperProvider(): IEntityAnimationMapperProvider<*, *> {
			return `resonator_combat_framework$getMapperProvider`()
		}
	}
}
