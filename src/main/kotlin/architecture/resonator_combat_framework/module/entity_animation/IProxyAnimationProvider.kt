package architecture.resonator_combat_framework.module.entity_animation

import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapperProvider

// 代理提供器。为实体提供动画控制器代理
interface IProxyAnimationProvider {
	fun `resonator_combat_framework$getMapperProvider`(): IEntityAnimationMapperProvider<*, *>

	companion object {
		fun IProxyAnimationProvider.getMapperProvider(): IEntityAnimationMapperProvider<*, *> {
			return `resonator_combat_framework$getMapperProvider`()
		}
	}
}