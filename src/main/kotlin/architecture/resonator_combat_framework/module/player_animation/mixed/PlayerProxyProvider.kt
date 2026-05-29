package architecture.resonator_combat_framework.module.player_animation.mixed

import architecture.resonator_combat_framework.module.player_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.player_animation.mapper.PlayerAnimationMapper

// 玩家代理提供器。Mixin 注入，为玩家实体提供动画控制器代理
interface PlayerProxyProvider {
	fun `resonator_combat_framework$getAnimationTransformer`(): PlayerAnimationMapper

	companion object {
		fun PlayerProxyProvider.getAnimationTransformer(): IAnimationMapper {
			return `resonator_combat_framework$getAnimationTransformer`()
		}
	}
}