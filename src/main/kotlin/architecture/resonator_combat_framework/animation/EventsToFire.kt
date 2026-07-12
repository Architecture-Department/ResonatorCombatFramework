package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.animation.keyframe_animation.ParticleEvent
import architecture.resonator_combat_framework.animation.keyframe_animation.SoundEvent
import architecture.resonator_combat_framework.animation.keyframe_animation.TimelineEvent

/**
 * 动画事件容器，由控制器筛选后交给管理器统一执行。
 */
data class EventsToFire
@JvmOverloads
constructor(
	/** 待触发的音效事件列表 */
	val sounds: List<SoundEvent> = emptyList(),
	/** 待触发的粒子事件列表 */
	val particles: List<ParticleEvent> = emptyList(),
	/** 待触发的时间线事件列表 */
	val timelines: List<TimelineEvent> = emptyList()
) {
	/** 是否有任意事件待触发 */
	val isEmpty: Boolean get() = sounds.isEmpty() && particles.isEmpty() && timelines.isEmpty()

	companion object {
		@JvmStatic
		val EMPTY = EventsToFire()
	}
}