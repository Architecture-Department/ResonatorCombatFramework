package architecture.resonator_combat_framework.event.listener.level

import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.LevelTickEvent

/**
 * 世界 tick 事件 —— 驱动世界级别的 MoLang 数据更新。
 */
@EventBusSubscriber(modid = RcfUtil.ID)
object LevelEvents {
	@SubscribeEvent
	fun onTickPre(event: LevelTickEvent.Pre) {
		val level = event.level
//		MolangDataHolder.of(level)
	}

	@SubscribeEvent
	fun onTickPost(event: LevelTickEvent.Post) {
	}
}
