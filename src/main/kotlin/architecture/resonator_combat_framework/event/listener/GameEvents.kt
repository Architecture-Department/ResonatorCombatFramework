package architecture.resonator_combat_framework.event.listener

import architecture.resonator_combat_framework.payload.toc.BoneConfigSynchPayload
import architecture.resonator_combat_framework.payload.toc.GeometryModelSynchPayload
import architecture.resonator_combat_framework.payload.toc.KeyframeAnimationSynchPayload
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.OnDatapackSyncEvent
import net.neoforged.neoforge.network.PacketDistributor


/**
 * 游戏通用事件 —— 处理数据包同步等全局性事件。
 * 在数据包同步时将骨骼配置、几何模型等数据从服务端推送到客户端。
 */
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
