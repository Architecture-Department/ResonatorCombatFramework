package architecture.resonator_combat_framework.module.player_animation.helper

import architecture.resonator_combat_framework.module.player_animation.mixed.PlayerProxyProvider.Companion.getAnimationTransformer
import architecture.resonator_combat_framework.module.player_animation.payload.AnimatePlayerPayload
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.PacketDistributor

object PlayerAnimationHelper {
	@JvmStatic
	fun Player.triggerPlayerAnimation(animId: String) {
		if (this is AbstractClientPlayer) {
			clientTriggerPlayerAnimation(animId)
		} else if (this is ServerPlayer) {
			serverPlayerAnimation(animId)
		}
	}

	@JvmStatic
	fun Player.stopPlayerAnimation() {
		if (this is AbstractClientPlayer) {
			clientStopPlayerAnimation()
		} else if (this is ServerPlayer) {
			serverStopPlayerAnimation()
		}
	}

	@JvmStatic
	fun AbstractClientPlayer.clientTriggerPlayerAnimation(animId: String) {
		getAnimationTransformer().trigger(animId)
	}

	@JvmStatic
	fun AbstractClientPlayer.clientStopPlayerAnimation() {
		getAnimationTransformer().stop()
	}

	@JvmStatic
	fun ServerPlayer.serverPlayerAnimation(animId: String) {
		getAnimationTransformer().trigger(animId)
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(this, AnimatePlayerPayload(animId, uuid))
	}

	@JvmStatic
	fun ServerPlayer.serverStopPlayerAnimation() {
		getAnimationTransformer().stop()
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
			this,
			AnimatePlayerPayload(AnimatePlayerPayload.STOP_MARKER, uuid)
		)
	}

	@JvmStatic
	fun AbstractClientPlayer.clientRequestPlayerAnimation(animId: String) {
		PacketDistributor.sendToServer(AnimatePlayerPayload(animId, uuid))
	}
}
