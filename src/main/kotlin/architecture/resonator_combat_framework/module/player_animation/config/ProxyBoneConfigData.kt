package architecture.resonator_combat_framework.module.player_animation.config


data class ProxyBoneConfigData(
	val bones: Map<String, ProxyBoneFlags> = emptyMap(),
	val timeline: List<ProxyTimelineEntry> = emptyList(),
	val transitionTicks: Int = DEFAULT_TRANSITION_TICKS
) {
	fun resolveBoneFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val result = bones.toMutableMap()
		for (entry in timeline) {
			if (animTime >= entry.from && animTime < entry.to) {
				result.putAll(entry.bones)
			}
		}
		return result
	}

	/** 返回当前动画所有可能影响的骨骼名称集合 (base bones + 所有 timeline entry 的 bones) */
	fun resolveCurrentBoneNames(): Set<String> {
		val names = bones.keys.toMutableSet()
		for (entry in timeline) {
			names.addAll(entry.bones.keys)
		}
		return names
	}

	companion object {
		@JvmField
		val EMPTY = ProxyBoneConfigData()

		const val DEFAULT_TRANSITION_TICKS: Int = 3
	}
}
