package architecture.resonator_combat_framework.module.player_animation.mixed

import architecture.resonator_combat_framework.module.player_animation.PlayerAnimationTransformer
import architecture.resonator_combat_framework.module.player_animation.api.IPlayerAnimator

interface PlayerProxyProvider {
	fun `resonator_combat_framework$getAnimationTransformer`(): PlayerAnimationTransformer

	companion object {
		fun PlayerProxyProvider.getAnimationTransformer(): IPlayerAnimator {
			return `resonator_combat_framework$getAnimationTransformer`()
		}
	}
}
