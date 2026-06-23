package architecture.resonator_combat_framework.config

import architecture.goldenboughs_lib.api.BasicConfig
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.neoforge.common.ModConfigSpec
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue

class RcfClientConfig(builder: ModConfigSpec.Builder) : BasicConfig(RcfUtil.ID, builder) {
	@JvmField
	val itemSwitchingAnimation: BooleanValue =
		define(false, "item_switching_animation", "物品切换动画")
}