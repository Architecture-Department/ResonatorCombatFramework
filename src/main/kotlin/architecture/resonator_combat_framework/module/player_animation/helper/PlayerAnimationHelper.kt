package architecture.resonator_combat_framework.module.player_animation.helper

import architecture.resonator_combat_framework.module.player_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.mixed.PlayerProxyProvider.Companion.getAnimationTransformer
import architecture.resonator_combat_framework.module.player_animation.payload.PausePlayerPayload
import architecture.resonator_combat_framework.module.player_animation.payload.PlayPlayerPayload
import architecture.resonator_combat_framework.module.player_animation.payload.ResumePlayerPayload
import architecture.resonator_combat_framework.module.player_animation.payload.StopPlayerPayload
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.PacketDistributor

object PlayerAnimationHelper {

	// ========== 触发 ==========

	@JvmStatic
	fun Player.triggerPlayerAnimation(animId: String) =
		triggerPlayerAnimation(AnimationPlayConfig.of(animId))

	@JvmStatic
	fun Player.triggerPlayerAnimation(animId: String, speedMultiplier: Float) =
		triggerPlayerAnimation(
			AnimationPlayConfig.builder(animId).speed(speedMultiplier).build()
		)

	@JvmStatic
	fun Player.triggerPlayerAnimation(animId: String, transitionTicks: Int, speedMultiplier: Float) =
		triggerPlayerAnimation(
			AnimationPlayConfig.builder(animId).fadeIn(transitionTicks).speed(speedMultiplier).build()
		)

	@JvmStatic
	fun Player.triggerPlayerAnimationForDuration(animId: String, durationTicks: Int, originalAnimLengthSec: Float) =
		triggerPlayerAnimation(
			AnimationPlayConfig.builder(animId).duration(durationTicks, originalAnimLengthSec).build()
		)

	@JvmStatic
	fun Player.triggerPlayerAnimationImmediate(animId: String) =
		triggerPlayerAnimation(AnimationPlayConfig.builder(animId).fadeIn(0).build())

	@JvmStatic
	fun Player.triggerPlayerAnimationImmediate(animId: String, speedMultiplier: Float) =
		triggerPlayerAnimation(
			AnimationPlayConfig.builder(animId).speed(speedMultiplier).fadeIn(0).build()
		)

	@JvmStatic
	fun Player.triggerPlayerAnimation(config: AnimationPlayConfig) {
		if (this is AbstractClientPlayer) clientTriggerPlayerAnimation(config)
		else if (this is ServerPlayer) serverTriggerPlayerAnimation(config)
	}

	@JvmStatic
	fun AbstractClientPlayer.clientTriggerPlayerAnimation(config: AnimationPlayConfig) {
		getAnimationTransformer().trigger(config)
	}

	@JvmStatic
	fun ServerPlayer.serverTriggerPlayerAnimation(config: AnimationPlayConfig) {
		getAnimationTransformer().trigger(config)
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
			this, PlayPlayerPayload(
				playerUuid = uuid, controllerName = null,
				animId = config.animId, animType = config.animType,
				speedMultiplier = config.resolveSpeedMultiplier(),
				startTime = config.startTime, endTime = config.endTime,
				fadeInTicks = config.fadeInTicks, fadeOutTicks = config.fadeOutTicks
			)
		)
	}

	// ========== 停止 ==========

	@JvmStatic
	fun Player.stopPlayerAnimation() {
		if (this is AbstractClientPlayer) clientStopPlayerAnimation()
		else if (this is ServerPlayer) serverStopPlayerAnimation()
	}

	@JvmStatic
	fun Player.stopPlayerAnimationImmediate() {
		if (this is AbstractClientPlayer) getAnimationTransformer().stopAllImmediate()
		else if (this is ServerPlayer) {
			getAnimationTransformer().stopAllImmediate()
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				this, StopPlayerPayload(uuid, null as String?, 0)
			)
		}
	}

	@JvmStatic
	fun AbstractClientPlayer.clientStopPlayerAnimation() {
		getAnimationTransformer().stopAll()
	}

	@JvmStatic
	fun ServerPlayer.serverStopPlayerAnimation() {
		getAnimationTransformer().stopAll()
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
			this, StopPlayerPayload(uuid, null as String?)
		)
	}

	// ========== 暂停 / 恢复 ==========

	@JvmStatic
	fun Player.pausePlayerAnimation() {
		if (this is AbstractClientPlayer) getAnimationTransformer().pause(IAnimationMapper.DEFAULT_CONTROLLER_NAME)
		else if (this is ServerPlayer) {
			getAnimationTransformer().pause(IAnimationMapper.DEFAULT_CONTROLLER_NAME)
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				this, PausePlayerPayload(uuid, null as String?)
			)
		}
	}

	@JvmStatic
	fun Player.resumePlayerAnimation() {
		if (this is AbstractClientPlayer) getAnimationTransformer().resume(IAnimationMapper.DEFAULT_CONTROLLER_NAME)
		else if (this is ServerPlayer) {
			getAnimationTransformer().resume(IAnimationMapper.DEFAULT_CONTROLLER_NAME)
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				this, ResumePlayerPayload(uuid, null as String?)
			)
		}
	}
}