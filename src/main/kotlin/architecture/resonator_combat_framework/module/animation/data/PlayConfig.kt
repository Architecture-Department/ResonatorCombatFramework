package architecture.resonator_combat_framework.module.animation.data

/**
 * 动画播放配置。
 *
 * @param playMode 动画类型：DEFAULT(按动画本身)、PLAY_ONCE(播放一次)、STOP_AT_LAST(停止于最后一帧)、LOOP(循环)
 * @param startTime 起始时间(秒)，默认 0
 * @param endTime 结束时间(秒)：0=持续到动画长度，负数=动画长度-N
 * @param speedMultiplier 动画倍数，默认 1.0
 * @param durationTime 在指定秒内播放完毕，与 speedMultiplier 互斥
 * @param originalAnimLengthSec 原始动画时长(秒)，配合 durationTime 使用
 * @param boneConfig 骨骼配置，null 则使用默认加载的配置
 * @param fadeInTime 淡入时间(秒)，-1 使用默认值
 * @param fadeOutTime 淡出时间(秒)，-1 使用默认值
 * @param mirror 是否镜像动画（左右翻转），默认 false
 */
data class PlayConfig(
	val playMode: PlayMode = PlayMode.DEFAULT,
	val startTime: Float = 0f,
	val endTime: Float = 0f,
	val speedMultiplier: Float = 1f,
	val durationTime: Float = 0f,
	val originalAnimLengthSec: Float = 0f,
	val boneConfig: BoneConfig? = null,
	val fadeInTime: Float = -1f,
	val fadeOutTime: Float = -1f,
	val mirror: Boolean = false
) {
	fun resolveSpeedMultiplier(): Float {
		if (durationTime > 0f && originalAnimLengthSec > 0f) {
			return originalAnimLengthSec / durationTime
		}
		return speedMultiplier
	}

	fun resolveFadeOutTime(defaultTime: Float): Float =
		if (fadeOutTime >= 0f) fadeOutTime else defaultTime

	fun resolveFadeInTime(defaultTime: Float): Float =
		if (fadeInTime >= 0f) fadeInTime else defaultTime

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
