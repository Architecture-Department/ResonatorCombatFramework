package architecture.resonator_combat_framework.module.animation.data

/**
 * 骨骼配置时间线条目。
 * 在 [from, to) 时间段内覆盖指定骨骼的标志配置。
 * 用于在动画的不同阶段动态切换骨骼的 blend/lock 行为。
 */
data class TimelineEntry(
	/** 时间线起始时间（秒，含） */
	val from: Float,
	/** 时间线结束时间（秒，不含） */
	val to: Float,
	/** 时间段内生效的骨骼标志配置 */
	val bones: Map<String, BoneFlags>
)
