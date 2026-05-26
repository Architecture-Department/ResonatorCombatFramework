package architecture.resonator_combat_framework.module.player_animation.config

import architecture.goldenboughs_lib.api.AllOpe

@AllOpe
interface ProxyBoneState {
	fun lockVanilla(): Boolean = true
}
