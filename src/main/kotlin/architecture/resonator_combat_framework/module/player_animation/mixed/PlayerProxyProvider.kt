package architecture.resonator_combat_framework.module.player_animation.mixed

import architecture.resonator_combat_framework.module.player_animation.core.PlayerAnimationTransformer

interface PlayerProxyProvider {
	fun `resonator_combat_framework$getAnimationTransformer`(): PlayerAnimationTransformer
}
