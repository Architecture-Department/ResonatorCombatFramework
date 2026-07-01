package architecture.resonator_combat_framework.module.entity_animation.helper

import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.IProxyAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayConfig
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
	fun Player.triggerPlayerAnima(
		animId: ResourceLocation,
		config: PlayConfig,
		controllerName: ResourceLocation = AnimationControllers.MAIN,
		isPayload: Boolean = true
	) {
		if (this is AbstractClientPlayer) {
			getMapperProvider().trigger(controllerName, animId, config)
			return
		}

		if (this !is ServerPlayer) return

		getMapperProvider().trigger(controllerName, animId, config)

		if (!isPayload) return

		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
			this, PlayPlayerPayload(
				playerUuid = uuid, controllerName = controllerName,
				animId = animId, animType = config.animType,
				speedMultiplier = config.resolveSpeedMultiplier(),
				startTime = config.startTime, endTime = config.endTime,
				fadeInTicks = config.fadeInTicks, fadeOutTicks = config.fadeOutTicks
			)
		)
	}

	/** 完整参数模式：速度 + 淡入 + 淡出 */
	@JvmStatic
	@JvmOverloads
	fun Player.triggerPlayerAnima(
		animId: ResourceLocation,
		speedMultiplier: Float = 1f,
		fadeInTicks: Int = -1,
		fadeOutTicks: Int = -1,
		isPayload: Boolean = true
	) {
		triggerPlayerAnima(
			animId,
			PlayConfig(
				speedMultiplier = speedMultiplier,
				fadeInTicks = fadeInTicks,
				fadeOutTicks = fadeOutTicks
			), isPayload = isPayload
		)
	}

	/** 过渡时间模式：过渡时间 + 速度 */
	@JvmStatic
	@JvmOverloads
	fun Player.triggerPlayerAnima(
		animId: ResourceLocation,
		transitionTicks: Int,
		speedMultiplier: Float = 1f,
		isPayload: Boolean = true
	) {
		triggerPlayerAnima(
			animId,
			PlayConfig(
				fadeInTicks = transitionTicks,
				speedMultiplier = speedMultiplier
			), isPayload = isPayload
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
		if (this is AbstractClientPlayer) getMapperProvider().stop(name, fadeOutTicks)
		else if (this is ServerPlayer) {
			getMapperProvider().stop(name, fadeOutTicks)
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
		if (this is AbstractClientPlayer) getMapperProvider().pause(name)
		else if (this is ServerPlayer) {
			getMapperProvider().pause(name)
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
		if (this is AbstractClientPlayer) getMapperProvider().resume(name)
		else if (this is ServerPlayer) {
			getMapperProvider().resume(name)
			if (isPayload) {
				PacketDistributor.sendToPlayersTrackingEntityAndSelf(
					this, ResumePlayerPayload(uuid, null as ResourceLocation?)
				)
			}
		}
	}
}

