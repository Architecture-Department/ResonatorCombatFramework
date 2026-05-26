package architecture.resonator_combat_framework.module.player_animation.config

import architecture.goldenboughs_lib.api.AllOpe

@ExposedCopyVisibility
@AllOpe
data class ProxyBoneConfigData
private constructor(
	val bones: Map<String, ProxyBoneFlags> = DEFAULT_FLAGS,
	val timeline: List<ProxyTimelineEntry> = emptyList(),
	val transitionTicks: Int = DEFAULT_TRANSITION_TICKS
) {
	companion object {
		@JvmField
		val DEFAULT_FLAGS = mapOf("head" to ProxyBoneFlags(mapOf("lock" to false)))

		@JvmField
		val EMPTY = ProxyBoneConfigData(v = null)

		const val DEFAULT_TRANSITION_TICKS: Int = 3
	}

	constructor(
		bones: Map<String, ProxyBoneFlags> = emptyMap(),
		timeline: List<ProxyTimelineEntry> = emptyList(),
		transitionTicks: Int = DEFAULT_TRANSITION_TICKS,
		v: Any? = null
	) : this(run {
		val merged = mutableMapOf<String, ProxyBoneFlags>().apply {
			putAll(DEFAULT_FLAGS)
			putAll(bones)
		}
		merged
	}, timeline, transitionTicks)

	fun resolveBoneFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val result = bones.toMutableMap()
		for (entry in timeline) {
			if (animTime >= entry.from && animTime < entry.to) {
				result.putAll(entry.bones)
			}
		}
		return result
	}

	fun resolveCurrentBoneNames(): Set<String> {
		val names = bones.keys.toMutableSet()
		for (entry in timeline) {
			names.addAll(entry.bones.keys)
		}
		return names
	}
}
