package architecture.resonator_combat_framework.module.entity_animation.animation

import net.minecraft.core.particles.ParticleOptions
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.LivingEntity

/**
 * 定时动画事件 —— 在动画播放的指定时间点执行效果。
 * 通过 [StaticAnimation.addEvent] 链式注册，无需创建子类。
 */
sealed class TimedEvent(
	/** 触发时间（秒） */
	val time: Float
) {
	/**
	 * 在指定时间点播放音效。
	 * @param time 触发时间（秒）
	 * @param sound 音效
	 * @param volume 音量
	 * @param pitch 音调
	 */
	class Sound(
		time: Float,
		val sound: SoundEvent,
		val volume: Float = 1f,
		val pitch: Float = 1f
	) : TimedEvent(time)

	/**
	 * 在指定时间点生成粒子。
	 * @param time 触发时间（秒）
	 * @param particle 粒子选项
	 * @param locator 骨骼定位器名称（空字符串表示实体位置）
	 */
	class Particle(
		time: Float,
		val particle: ParticleOptions,
		val locator: String = ""
	) : TimedEvent(time)

	/**
	 * 在指定时间点执行自定义回调。
	 * @param time 触发时间（秒）
	 * @param handler 回调函数：(实体, 动画时间) -> Unit
	 */
	class Callback(
		time: Float,
		val handler: (LivingEntity, Float) -> Unit
	) : TimedEvent(time)
}
