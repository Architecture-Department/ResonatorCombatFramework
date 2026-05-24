package architecture.resonator_combat_framework.module.player_animation.mixed

import architecture.resonator_combat_framework.module.player_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.player_animation.mapper.PlayerAnimationMapper

interface PlayerProxyProvider {
	fun `resonator_combat_framework$getAnimationTransformer`(): PlayerAnimationMapper

	companion object {
		fun PlayerProxyProvider.getAnimationTransformer(): IAnimationMapper {
			return `resonator_combat_framework$getAnimationTransformer`()
		}
	}
}
