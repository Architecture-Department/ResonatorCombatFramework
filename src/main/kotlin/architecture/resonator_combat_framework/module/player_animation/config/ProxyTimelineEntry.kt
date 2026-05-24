package architecture.resonator_combat_framework.module.player_animation.config

data class ProxyTimelineEntry(
	val from: Float,
	val to: Float,
	val bones: Map<String, ProxyBoneFlags>
)