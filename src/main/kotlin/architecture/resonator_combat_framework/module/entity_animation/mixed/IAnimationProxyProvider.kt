package architecture.resonator_combat_framework.module.entity_animation.mixed

import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapperProvider

// 代理提供器。为实体提供动画控制器代理
interface IAnimationProxyProvider {
	fun `resonator_combat_framework$getAnimationTransformer`(): IEntityAnimationMapperProvider<*, *>

	companion object {
		fun IAnimationProxyProvider.getAnimationTransformer(): IEntityAnimationMapperProvider<*, *> {
			return `resonator_combat_framework$getAnimationTransformer`()
		}
	}
}

