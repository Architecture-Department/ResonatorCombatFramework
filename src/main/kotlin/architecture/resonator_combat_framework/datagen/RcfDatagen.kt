package architecture.resonator_combat_framework.datagen

import architecture.goldenboughs_lib.util.datagen.buildClient
import architecture.resonator_combat_framework.datagen.i18n.RcfZhCn
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.data.event.GatherDataEvent

/**
 * 数据生成主类
 */
@EventBusSubscriber(modid = RcfUtil.ID)
object RcfDatagen {
	@SubscribeEvent
	fun gatherData(event: GatherDataEvent) {
		val generator = event.generator
		val output = generator.packOutput
		val completableFuture = event.lookupProvider
		val existingFileHelper = event.existingFileHelper

		// 服务端数据生成

		// 客户端数据生成
		event.buildClient(
			RcfZhCn(output),
		)
	}
}
