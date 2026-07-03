package architecture.resonator_combat_framework.module.animation.helper

import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.animation.data.PlayConfig
import architecture.resonator_combat_framework.module.animation.network.command.PausePlayerPayload
import architecture.resonator_combat_framework.module.animation.network.command.PlayPlayerPayload
import architecture.resonator_combat_framework.module.animation.network.command.ResumePlayerPayload
import architecture.resonator_combat_framework.module.animation.network.command.StopPlayerPayload
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.PacketDistributor

/**
 * 玩家动画辅助工具，提供播放、停止、暂停和恢复动画的扩展方法。
 *
 * 所有方法同时支持客户端（[AbstractClientPlayer]）和服务端（[ServerPlayer]）操作，
 * 服务端操作会自动向追踪该实体的所有玩家同步网络数据包。
 */
object PlayerAnimationHelper {

	// ========== 动画触发（三种重载） ==========

	/**
	 * 通过完整的 [PlayConfig] 配置触发玩家动画。
	 * @param animId 动画资源 ID
	 * @param config 播放配置（速度、淡入淡出等）
	 * @param controllerName 目标控制器名称，默认为 [AnimationControllers.MAIN]
	 * @param isPayload 是否同步网络数据包（仅服务端有效），默认为 true
	 */
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
				animId = animId, playMode = config.playMode,
				speedMultiplier = config.resolveSpeedMultiplier(),
				startTime = config.startTime, endTime = config.endTime,
				fadeInTicks = config.fadeInTicks, fadeOutTicks = config.fadeOutTicks
			)
		)
	}

	/**
	 * 通过简化参数触发玩家动画（速度 + 淡入 + 淡出）。
	 * @param animId 动画资源 ID
	 * @param speedMultiplier 播放速度倍率，默认为 1.0
	 * @param fadeInTicks 淡入过渡刻数，-1 表示使用默认值
	 * @param fadeOutTicks 淡出过渡刻数，-1 表示使用默认值
	 * @param isPayload 是否同步网络数据包
	 */
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

	/**
	 * 通过过渡时间和速度触发玩家动画。
	 * @param animId 动画资源 ID
	 * @param transitionTicks 过渡（淡入）刻数
	 * @param speedMultiplier 播放速度倍率，默认为 1.0
	 * @param isPayload 是否同步网络数据包
	 */
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

	/**
	 * 停止指定控制器的动画播放。
	 * @param name 目标控制器名称，默认为 [AnimationControllers.MAIN]
	 * @param fadeOutTicks 淡出刻数，-1 表示使用默认值
	 * @param isPayload 是否同步网络数据包
	 */
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

	/**
	 * 暂停指定控制器的动画播放。
	 * @param name 目标控制器名称，默认为 [AnimationControllers.MAIN]
	 * @param isPayload 是否同步网络数据包
	 */
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

	/**
	 * 恢复指定控制器的动画播放。
	 * @param name 目标控制器名称，默认为 [AnimationControllers.MAIN]
	 * @param isPayload 是否同步网络数据包
	 */
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
