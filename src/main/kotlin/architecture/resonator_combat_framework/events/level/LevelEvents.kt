package architecture.resonator_combat_framework.events.level

import architecture.resonator_combat_framework.module.animation.molang.MolangData
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.LevelTickEvent

@EventBusSubscriber(modid = RcfUtil.ID)
object LevelEvents {
	@SubscribeEvent
	fun onTickPre(event: LevelTickEvent.Pre) {
		val molangData = MolangData.of(event.level)
		// TODO 补充
	}
}