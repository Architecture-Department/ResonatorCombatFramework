package architecture.resonator_combat_framework.module.entity_animation.controller

import architecture.resonator_combat_framework.module.entity_animation.engine.BrAnimationParticle
import architecture.resonator_combat_framework.module.entity_animation.engine.BrAnimationSound
import architecture.resonator_combat_framework.module.entity_animation.engine.BrAnimationTimeline

/**
 * 动画事件容器，由控制器筛选后交给管理器统一执行。
 */
data class AnimationEventsToFire(
	/** 待触发的音效事件列表 */
	val sounds: List<BrAnimationSound> = emptyList(),
	/** 待触发的粒子事件列表 */
	val particles: List<BrAnimationParticle> = emptyList(),
	/** 待触发的时间线事件列表 */
	val timelines: List<BrAnimationTimeline> = emptyList()
) {
	/** 是否有任意事件待触发 */
	val isEmpty: Boolean get() = sounds.isEmpty() && particles.isEmpty() && timelines.isEmpty()
}
