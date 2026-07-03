package architecture.resonator_combat_framework.events

import architecture.resonator_combat_framework.module.animation.network.keyframe_animation.BoneConfigSynchPayload
import architecture.resonator_combat_framework.module.animation.network.keyframe_animation.KeyframeAnimationSynchPayload
import architecture.resonator_combat_framework.module.animation.network.keyframe_animation.GeometryModelSynchPayload
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.OnDatapackSyncEvent
import net.neoforged.neoforge.network.PacketDistributor


@EventBusSubscriber(modid = RcfUtil.ID)
object GameEvents {
	/**
	 * 数据包同步回调：向相关玩家发送骨骼配置和几何模型同步数据包。
	 *
	 * @param event 数据包同步事件
	 */
	@SubscribeEvent
	fun onDatapackSync(event: OnDatapackSyncEvent) {
		if (event.relevantPlayers.toList().isEmpty() || event.relevantPlayers.findFirst().isEmpty) return
		val payload = KeyframeAnimationSynchPayload()
		val payload1 = GeometryModelSynchPayload()
		val payload2 = BoneConfigSynchPayload()
		event.relevantPlayers.forEach { player ->
			if (player == null || player.connection.getConnection().isMemoryConnection) return@forEach
			PacketDistributor.sendToPlayer(player, payload, payload1, payload2)
		}
	}
}
