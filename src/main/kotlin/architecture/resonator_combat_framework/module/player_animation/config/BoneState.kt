package architecture.resonator_combat_framework.module.player_animation.config

import architecture.goldenboughs_lib.api.AllOpe

@AllOpe
interface BoneState {
	fun lockVanilla(): Boolean = false
}
