package architecture.resonator_combat_framework.events

import architecture.goldenboughs_lib.util.LibUtil
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.LevelTickEvent

@EventBusSubscriber(modid = LibUtil.ID)
object LevelEvents {
	@SubscribeEvent
	fun onTickPre(event: LevelTickEvent.Pre) {
		val molangData = MolangData.of(event.level)
		// TODO 补充
	}
}