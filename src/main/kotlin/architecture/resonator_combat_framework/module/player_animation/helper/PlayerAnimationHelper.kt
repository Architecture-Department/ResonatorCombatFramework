package architecture.resonator_combat_framework.module.player_animation.helper

import architecture.resonator_combat_framework.module.player_animation.mixed.PlayerProxyProvider
import architecture.resonator_combat_framework.module.player_animation.payload.AnimatePlayerPayload
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.player.RemotePlayer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.PacketDistributor

object PlayerAnimationHelper {
	// 客户端：触发动画
	@JvmStatic
	fun triggerPlayerAnimation(player: Player, id: String) {
		if (player !is LocalPlayer && player !is RemotePlayer) return
		(player as PlayerProxyProvider).`resonator_combat_framework$getAnimationTransformer`().trigger(id)
	}

	// 客户端：停止动画
	@JvmStatic
	fun stopPlayerAnimation(player: Player) {
		if (player !is LocalPlayer && player !is RemotePlayer) return
		(player as PlayerProxyProvider).`resonator_combat_framework$getAnimationTransformer`().stop()
	}

	// 服务端→所有客户端：广播触发
	@JvmStatic
	fun pushPlayerAnimation(player: Player, id: String) {
		if (player !is ServerPlayer) return
		(player as PlayerProxyProvider).`resonator_combat_framework$getAnimationTransformer`().trigger(id)
		PacketDistributor.sendToAllPlayers(AnimatePlayerPayload(id, player.uuid))
	}

	// 服务端→所有客户端：广播停止
	@JvmStatic
	fun pushStopPlayerAnimation(player: Player) {
		if (player !is ServerPlayer) return
		(player as PlayerProxyProvider).`resonator_combat_framework$getAnimationTransformer`().stop()
		PacketDistributor.sendToAllPlayers(AnimatePlayerPayload("##stop##", player.uuid))
	}

	// 客户端→服务端：请求广播
	@JvmStatic
	fun requestPlayerAnimation(target: Player, id: String) {
		PacketDistributor.sendToServer(AnimatePlayerPayload(id, target.uuid))
	}
}
