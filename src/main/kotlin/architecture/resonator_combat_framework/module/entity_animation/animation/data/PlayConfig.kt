package architecture.resonator_combat_framework.module.entity_animation.animation.data

/**
 * 动画播放配置。
 *
 * @param playMode 动画类型：DEFAULT(按动画本身)、PLAY_ONCE(播放一次)、STOP_AT_LAST(停止于最后一帧)、LOOP(循环)
 * @param startTime 起始时间(tick)，默认 0
 * @param endTime 结束时间(tick)：0=持续到动画长度，负数=动画长度-N
 * @param speedMultiplier 动画倍数，默认 1.0
 * @param durationTicks 在指定 tick 内播放完毕，与 speedMultiplier 互斥
 * @param originalAnimLengthSec 原始动画时长(秒)，配合 durationTicks 使用
 * @param boneConfig 骨骼配置，null 则使用默认加载的配置
 * @param fadeInTicks 淡入时间(tick)，-1 使用默认值
 * @param fadeOutTicks 淡出时间(tick)，-1 使用默认值
 * @param mirror 是否镜像动画（左右翻转），默认 false
 */
data class PlayConfig(
	val playMode: PlayMode = PlayMode.DEFAULT,
	val startTime: Int = 0,
	val endTime: Int = 0,
	val speedMultiplier: Float = 1f,
	val durationTicks: Int = 0,
	val originalAnimLengthSec: Float = 0f,
	val boneConfig: BoneConfig? = null,
	val fadeInTicks: Int = -1,
	val fadeOutTicks: Int = -1,
	val mirror: Boolean = false
) {
	fun resolveSpeedMultiplier(): Float {
		if (durationTicks > 0 && originalAnimLengthSec > 0f) {
			return originalAnimLengthSec / (durationTicks / 20f)
		}
		return speedMultiplier
	}

	fun resolveFadeOutTicks(defaultTicks: Int): Int =
		if (fadeOutTicks >= 0) fadeOutTicks else defaultTicks

	fun resolveFadeInTicks(defaultTicks: Int): Int =
		if (fadeInTicks >= 0) fadeInTicks else defaultTicks

	companion object {
		@JvmField
		val EMPTY = PlayConfig()

		@JvmStatic
		fun of() = PlayConfig()
	}
}

enum class PlayMode {
	/** 按动画本身的 loop 类型 */
	DEFAULT,

	/** 强制播放一次后停止 */
	PLAY_ONCE,

	/** 播放一次，停止于最后一帧（保持姿态不淡出）*/
	STOP_AT_LAST,

	/** 强制循环播放 */
	LOOP
}
