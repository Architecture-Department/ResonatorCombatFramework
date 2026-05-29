package architecture.resonator_combat_framework.module.player_animation.config

// 骨骼配置时间线条目

data class ProxyTimelineEntry(
	val from: Float,
	val to: Float,
	val bones: Map<String, ProxyBoneFlags>
)
