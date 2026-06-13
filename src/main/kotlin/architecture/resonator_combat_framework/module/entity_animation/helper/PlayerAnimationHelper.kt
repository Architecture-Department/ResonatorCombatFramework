package architecture.resonator_combat_framework.module.entity_animation.helper

import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.mixed.IAnimationProxyProvider.Companion.getAnimationTransformer
import architecture.resonator_combat_framework.module.entity_animation.network.PausePlayerPayload
import architecture.resonator_combat_framework.module.entity_animation.network.PlayPlayerPayload
import architecture.resonator_combat_framework.module.entity_animation.network.ResumePlayerPayload
import architecture.resonator_combat_framework.module.entity_animation.network.StopPlayerPayload
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.PacketDistributor

object PlayerAnimationHelper {

	// ========== 触发（三种模式） ==========

	/** 完整数据类模式 */
	@JvmStatic
	@JvmOverloads
	fun Player.triggerPlayerAnima(config: AnimationPlayData, isPayload: Boolean = true) {
		if (this is AbstractClientPlayer) {
			getAnimationTransformer().trigger(config)
		} else if (this is ServerPlayer) {
			getAnimationTransformer().trigger(config)
			if (isPayload) {
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
		}
	}

	/** 完整参数模式：速度 + 淡入 + 淡出 */
	@JvmStatic
	@JvmOverloads
	fun Player.triggerPlayerAnima(
		animId: String,
		speedMultiplier: Float = 1f,
		fadeInTicks: Int = -1,
		fadeOutTicks: Int = -1,
		isPayload: Boolean = true
	) {
		triggerPlayerAnima(
			AnimationPlayData.builder(animId)
				.speed(speedMultiplier)
				.fadeIn(fadeInTicks)
				.fadeOut(fadeOutTicks)
				.build(), isPayload
		)
	}

	/** 过渡时间模式：过渡时间 + 速度 */
	@JvmStatic
	@JvmOverloads
	fun Player.triggerPlayerAnima(
		animId: String,
		transitionTicks: Int,
		speedMultiplier: Float = 1f,
		isPayload: Boolean = true
	) {
		triggerPlayerAnima(
			AnimationPlayData.builder(animId)
				.fadeIn(transitionTicks)
				.speed(speedMultiplier)
				.build(), isPayload
		)
	}

	// ========== 停止 ==========

	@JvmStatic
	@JvmOverloads
	fun Player.stopAnima(
		name: ResourceLocation = AnimationControllers.MAIN,
		fadeOutTicks: Int = -1,
		isPayload: Boolean = true
	) {
		if (this is AbstractClientPlayer) getAnimationTransformer().stop(name, fadeOutTicks)
		else if (this is ServerPlayer) {
			getAnimationTransformer().stop(name, fadeOutTicks)
			if (isPayload) {
				PacketDistributor.sendToPlayersTrackingEntityAndSelf(
					this, StopPlayerPayload(uuid, null as ResourceLocation?, fadeOutTicks)
				)
			}
		}
	}

	// ========== 暂停 / 恢复 ==========

	@JvmStatic
	@JvmOverloads
	fun Player.pauseAnima(name: ResourceLocation = AnimationControllers.MAIN, isPayload: Boolean = true) {
		if (this is AbstractClientPlayer) getAnimationTransformer().pause(name)
		else if (this is ServerPlayer) {
			getAnimationTransformer().pause(name)
			if (isPayload) {
				PacketDistributor.sendToPlayersTrackingEntityAndSelf(
					this, PausePlayerPayload(uuid, null as ResourceLocation?)
				)
			}
		}
	}

	@JvmStatic
	@JvmOverloads
	fun Player.resumeAnima(name: ResourceLocation = AnimationControllers.MAIN, isPayload: Boolean = true) {
		if (this is AbstractClientPlayer) getAnimationTransformer().resume(name)
		else if (this is ServerPlayer) {
			getAnimationTransformer().resume(name)
			if (isPayload) {
				PacketDistributor.sendToPlayersTrackingEntityAndSelf(
					this, ResumePlayerPayload(uuid, null as ResourceLocation?)
				)
			}
		}
	}
}

