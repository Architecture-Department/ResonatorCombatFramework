package architecture.resonator_combat_framework.module.entity_animation.data

import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.mixed.PlayerProxyProvider.Companion.getAnimationTransformer
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player

/**
 * 动画播放配置。
 *
 * @param animId 动画名称
 * @param controllerName 控制器 ID，默认使用主控制器
 * @param animType 动画类型：DEFAULT(按动画本身)、PLAY_ONCE(播放一次)、STOP_AT_LAST(停止于最后一帧)、LOOP(循环)
 * @param startTime 起始时间(tick)，默认 0
 * @param endTime 结束时间(tick)：0=持续到动画长度，负数=动画长度-N
 * @param speedMultiplier 动画倍数，默认 1.0
 * @param durationTicks 在指定 tick 内播放完毕，与 speedMultiplier 互斥
 * @param originalAnimLengthSec 原始动画时长(秒)，配合 durationTicks 使用
 * @param boneConfig 骨骼配置，null 则使用默认加载的配置
 * @param fadeInTicks 淡入时间(tick)，-1 使用默认值
 * @param fadeOutTicks 淡出时间(tick)，-1 使用默认值
 */
data class AnimationPlayData(
	val animId: String,
	val controllerName: ResourceLocation = AnimationControllers.MAIN,
	val animType: AnimType = AnimType.DEFAULT,
	val startTime: Int = 0,
	val endTime: Int = 0,
	val speedMultiplier: Float = 1f,
	val durationTicks: Int = 0,
	val originalAnimLengthSec: Float = 0f,
	val boneConfig: ProxyBoneConfigData? = null,
	val fadeInTicks: Int = -1,
	val fadeOutTicks: Int = -1
) {
	/** 解析实际速度倍数：durationTicks > 0 时自动计算 */
	fun resolveSpeedMultiplier(): Float {
		if (durationTicks > 0 && originalAnimLengthSec > 0f) {
			return originalAnimLengthSec / (durationTicks / 20f)
		}
		return speedMultiplier
	}

	/** 解析实际淡出 tick 数 */
	fun resolveFadeOutTicks(defaultTicks: Int): Int =
		if (fadeOutTicks >= 0) fadeOutTicks else defaultTicks

	/** 解析实际淡入 tick 数 */
	fun resolveFadeInTicks(defaultTicks: Int): Int =
		if (fadeInTicks >= 0) fadeInTicks else defaultTicks

	fun Player.playAnimation() {
		getAnimationTransformer().trigger(this@AnimationPlayData)
	}

	companion object {
		@JvmField
		val EMPTY = AnimationPlayData("")

		/** 快速构造：仅动画名称 */
		@JvmStatic
		fun of(animId: String) = AnimationPlayData(animId = animId)

		/** 建造者入口 */
		@JvmStatic
		fun builder(animId: String) = Builder(animId)
	}

	class Builder(private val animId: String) {
		private var controllerName: ResourceLocation = AnimationControllers.MAIN
		private var animType: AnimType = AnimType.DEFAULT
		private var startTime: Int = 0
		private var endTime: Int = 0
		private var speedMultiplier: Float = 1f
		private var durationTicks: Int = 0
		private var originalAnimLengthSec: Float = 0f
		private var boneConfig: ProxyBoneConfigData? = null
		private var fadeInTicks: Int = -1
		private var fadeOutTicks: Int = -1

		fun controller(name: ResourceLocation) = apply { controllerName = name }
		fun type(type: AnimType) = apply { animType = type }
		fun startAt(tick: Int) = apply { startTime = tick }
		fun endAt(tick: Int) = apply { endTime = tick }
		fun speed(multiplier: Float) = apply { speedMultiplier = multiplier }
		fun duration(ticks: Int, originalLengthSec: Float) = apply {
			durationTicks = ticks
			originalAnimLengthSec = originalLengthSec
		}

		fun boneConfig(config: ProxyBoneConfigData) = apply { boneConfig = config }
		fun fadeIn(ticks: Int) = apply { fadeInTicks = ticks }
		fun fadeOut(ticks: Int) = apply { fadeOutTicks = ticks }

		fun build() = AnimationPlayData(
			animId = animId,
			controllerName = controllerName,
			animType = animType,
			startTime = startTime,
			endTime = endTime,
			speedMultiplier = speedMultiplier,
			durationTicks = durationTicks,
			originalAnimLengthSec = originalAnimLengthSec,
			boneConfig = boneConfig,
			fadeInTicks = fadeInTicks,
			fadeOutTicks = fadeOutTicks
		)
	}
}

/** 动画播放类型 */
enum class AnimType {
	/** 按动画本身的 loop 类型 */
	DEFAULT,

	/** 强制播放一次后停止 */
	PLAY_ONCE,

	/** 播放一次，停止于最后一帧（保持姿态不淡出）*/
	STOP_AT_LAST,

	/** 强制循环播放 */
	LOOP
}

