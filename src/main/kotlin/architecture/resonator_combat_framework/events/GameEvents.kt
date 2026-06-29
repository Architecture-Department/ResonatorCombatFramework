package architecture.resonator_combat_framework.events

import architecture.resonator_combat_framework.module.entity_animation.network.BedrockAnimationDataSynchsPayload
import architecture.resonator_combat_framework.module.entity_animation.network.BedrockModelDataSynchsPayload
import architecture.resonator_combat_framework.module.entity_animation.network.ProxyBoneConfigDataDataSynchsPayload
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.OnDatapackSyncEvent
import net.neoforged.neoforge.network.PacketDistributor


@EventBusSubscriber(modid = RcfUtil.ID)
object GameEvents {
	@SubscribeEvent
	fun onDatapackSync(event: OnDatapackSyncEvent) {
		if (event.relevantPlayers.toList().isEmpty() || event.relevantPlayers.findFirst().isEmpty) return
		val payload = BedrockAnimationDataSynchsPayload()
		val payload1 = BedrockModelDataSynchsPayload()
		val payload2 = ProxyBoneConfigDataDataSynchsPayload()
		event.relevantPlayers.forEach { player ->
			if (player == null || player.connection.getConnection().isMemoryConnection) return@forEach
			PacketDistributor.sendToPlayer(player, payload, payload1, payload2)
		}
	}
}