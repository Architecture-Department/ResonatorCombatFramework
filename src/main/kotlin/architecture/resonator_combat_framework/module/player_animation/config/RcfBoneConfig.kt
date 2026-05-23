package architecture.resonator_combat_framework.module.player_animation.config


data class RcfBoneConfig(
	val bones: Map<String, RcfBoneFlags> = emptyMap(),
	val timeline: List<RcfTimelineEntry> = emptyList(),
	val transitionTicks: Int = DEFAULT_TRANSITION_TICKS
) {
	fun resolveBoneFlags(animTime: Float): Map<String, RcfBoneFlags> {
		val result = bones.toMutableMap()
		for (entry in timeline) {
			if (animTime >= entry.from && animTime < entry.to) {
				result.putAll(entry.bones)
			}
		}
		return result
	}

	companion object {
		@JvmField
		val EMPTY = RcfBoneConfig()

		const val DEFAULT_TRANSITION_TICKS: Int = 3
	}
}
