package architecture.resonator_combat_framework.module.player_animation.helper

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.GeoPlayerAnimations
import architecture.resonator_combat_framework.module.player_animation.payload.toc.AnimatePlayerPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.PacketDistributor

object PlayerAnimationHelper {
	@JvmStatic
	fun triggerPlayerAnimation(player: Player, id: String) {
//		if (player !is LocalPlayer && player !is RemotePlayer) return
		val consumer = GeoPlayerAnimations.get(id)
		if (consumer == null) {
			Rcf.LOGGER.error("Could not find any player animation with id: {}", id)
			return
		}
		player.`resonator_combat_framework$getAnimationGeoPlayer`().startProxy(consumer)
	}

	@JvmStatic
	fun pushPlayerAnimation(player: Player, id: String) {
		if (player !is ServerPlayer) return
		PacketDistributor.sendToAllPlayers(AnimatePlayerPayload(id, player.getUUID()))
	}
}