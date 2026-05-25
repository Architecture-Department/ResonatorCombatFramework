package architecture.resonator_combat_framework.module.player_animation.helper

import architecture.resonator_combat_framework.module.player_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.mixed.PlayerProxyProvider.Companion.getAnimationTransformer
import architecture.resonator_combat_framework.module.player_animation.payload.AnimatePlayerPayload
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.PacketDistributor

object PlayerAnimationHelper {

	// ═══════════════ 便捷：AnimId 触发 ═══════════════

	@JvmStatic
	fun Player.triggerPlayerAnimation(animId: String) =
		triggerPlayerAnimation(AnimationPlayConfig.of(animId))

	@JvmStatic
	fun Player.triggerPlayerAnimation(animId: String, speedMultiplier: Float) =
		triggerPlayerAnimation(AnimationPlayConfig.of(animId).copy(speedMultiplier = speedMultiplier))

	@JvmStatic
	fun Player.triggerPlayerAnimation(animId: String, transitionTicks: Int, speedMultiplier: Float) =
		triggerPlayerAnimation(
			AnimationPlayConfig.of(animId).copy(fadeInTicks = transitionTicks, speedMultiplier = speedMultiplier)
		)

	@JvmStatic
	fun Player.triggerPlayerAnimationForDuration(animId: String, durationTicks: Int, originalAnimLengthSec: Float) =
		triggerPlayerAnimation(AnimationPlayConfig.builder(animId).duration(durationTicks, originalAnimLengthSec).build())

	// ═══════════════ 核心：Config 触发 ═══════════════

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
			this,
			AnimatePlayerPayload(
				config.animId, uuid, config.resolveSpeedMultiplier(),
				config.durationTicks > 0, config.durationTicks, config.originalAnimLengthSec
			)
		)
	}

	// ═══════════════ 停止 ═══════════════

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
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(this, AnimatePlayerPayload.stop())
		}
	}

	@JvmStatic
	fun AbstractClientPlayer.clientStopPlayerAnimation() {
		getAnimationTransformer().stopAll()
	}

	@JvmStatic
	fun ServerPlayer.serverStopPlayerAnimation() {
		getAnimationTransformer().stopAll()
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(this, AnimatePlayerPayload.stop())
	}

	// ═══════════════ 暂停 / 恢复 ═══════════════

	@JvmStatic
	fun Player.pausePlayerAnimation() {
		if (this is AbstractClientPlayer) getAnimationTransformer().pause(IAnimationMapper.DEFAULT_CONTROLLER_NAME)
		else if (this is ServerPlayer) getAnimationTransformer().pause(IAnimationMapper.DEFAULT_CONTROLLER_NAME)
	}

	@JvmStatic
	fun Player.resumePlayerAnimation() {
		if (this is AbstractClientPlayer) getAnimationTransformer().resume(IAnimationMapper.DEFAULT_CONTROLLER_NAME)
		else if (this is ServerPlayer) getAnimationTransformer().resume(IAnimationMapper.DEFAULT_CONTROLLER_NAME)
	}
}
