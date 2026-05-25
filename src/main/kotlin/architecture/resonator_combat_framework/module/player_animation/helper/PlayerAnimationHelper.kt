package architecture.resonator_combat_framework.module.player_animation.helper

import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.mixed.PlayerProxyProvider.Companion.getAnimationTransformer
import architecture.resonator_combat_framework.module.player_animation.payload.AnimatePlayerPayload
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.PacketDistributor

object PlayerAnimationHelper {
	// ---- 基础触发 ----

	@JvmStatic
	fun Player.triggerPlayerAnimation(animId: String) {
		triggerPlayerAnimation(animId, 1f)
	}

	@JvmStatic
	fun Player.triggerPlayerAnimation(animId: String, speedMultiplier: Float) {
		if (this is AbstractClientPlayer) {
			clientTriggerPlayerAnimation(animId, speedMultiplier)
		} else if (this is ServerPlayer) {
			serverPlayerAnimation(animId, speedMultiplier)
		}
	}

	@JvmStatic
	fun Player.triggerPlayerAnimationForDuration(animId: String, durationTicks: Int, originalAnimLengthSec: Float) {
		if (this is AbstractClientPlayer) {
			clientTriggerPlayerAnimationForDuration(animId, durationTicks, originalAnimLengthSec)
		} else if (this is ServerPlayer) {
			serverPlayerAnimationForDuration(animId, durationTicks, originalAnimLengthSec)
		}
	}

	// ---- 详细配置播放 ----

	@JvmStatic
	fun Player.triggerPlayerAnimation(config: AnimationPlayConfig) {
		if (this is AbstractClientPlayer) clientTriggerPlayerAnimation(config)
		else if (this is ServerPlayer) serverPlayerAnimation(config)
	}

	@JvmStatic
	fun AbstractClientPlayer.clientTriggerPlayerAnimation(config: AnimationPlayConfig) {
		getAnimationTransformer().trigger(config)
	}

	@JvmStatic
	fun ServerPlayer.serverPlayerAnimation(config: AnimationPlayConfig) {
		getAnimationTransformer().trigger(config)
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
			this,
			AnimatePlayerPayload(config.animId, uuid, config.resolveSpeedMultiplier(), false, 0, 0f)
		)
	}

	// ---- 停止 ----

	@JvmStatic
	fun Player.stopPlayerAnimation() {
		if (this is AbstractClientPlayer) {
			clientStopPlayerAnimation()
		} else if (this is ServerPlayer) {
			serverStopPlayerAnimation()
		}
	}

	// ---- 客户端 ----

	@JvmStatic
	fun AbstractClientPlayer.clientTriggerPlayerAnimation(animId: String) {
		clientTriggerPlayerAnimation(animId, 1f)
	}

	@JvmStatic
	fun AbstractClientPlayer.clientTriggerPlayerAnimation(animId: String, speedMultiplier: Float) {
		// 必须在 trigger 前设置 speedMultiplier
		getAnimationTransformer().getController().speedMultiplier = speedMultiplier
		getAnimationTransformer().trigger(animId)
	}

	@JvmStatic
	fun AbstractClientPlayer.clientTriggerPlayerAnimationForDuration(
		animId: String,
		durationTicks: Int,
		originalAnimLengthSec: Float
	) {
		getAnimationTransformer().getController().triggerForDuration(animId, 3, durationTicks, originalAnimLengthSec)
	}

	@JvmStatic
	fun AbstractClientPlayer.clientStopPlayerAnimation() {
		getAnimationTransformer().stopAll()
	}

	// ---- 服务端 ----

	@JvmStatic
	fun ServerPlayer.serverPlayerAnimation(animId: String) {
		serverPlayerAnimation(animId, 1f)
	}

	@JvmStatic
	fun ServerPlayer.serverPlayerAnimation(animId: String, speedMultiplier: Float) {
		getAnimationTransformer().getController().speedMultiplier = speedMultiplier
		getAnimationTransformer().trigger(animId)
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
			this,
			AnimatePlayerPayload(animId, uuid, speedMultiplier, false, 0, 0f)
		)
	}

	@JvmStatic
	fun ServerPlayer.serverPlayerAnimationForDuration(animId: String, durationTicks: Int, originalAnimLengthSec: Float) {
		getAnimationTransformer().getController().triggerForDuration(animId, 3, durationTicks, originalAnimLengthSec)
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
			this,
			AnimatePlayerPayload(animId, uuid, 1f, true, durationTicks, originalAnimLengthSec)
		)
	}

	@JvmStatic
	fun ServerPlayer.serverStopPlayerAnimation() {
		getAnimationTransformer().stopAll()
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
			this,
			AnimatePlayerPayload(AnimatePlayerPayload.STOP_MARKER, uuid)
		)
	}

	// ---- 请求 ----

	@JvmStatic
	fun AbstractClientPlayer.clientRequestPlayerAnimation(animId: String) {
		PacketDistributor.sendToServer(AnimatePlayerPayload(animId, uuid, 1f, false, 0, 0f))
	}
}
