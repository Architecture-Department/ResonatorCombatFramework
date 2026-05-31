package architecture.resonator_combat_framework.module.player_animation.config

import architecture.goldenboughs_lib.api.AllOpe

// 骨骼配置数据。包含过渡时间、骨骼标志、时间线
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
		val EMPTY = create(emptyMap(), emptyList(), DEFAULT_TRANSITION_TICKS)

		const val DEFAULT_TRANSITION_TICKS: Int = 3

		fun create(
			bones: Map<String, ProxyBoneFlags>,
			timeline: List<ProxyTimelineEntry>,
			transitionTicks: Int
		): ProxyBoneConfigData {
			val merged = mutableMapOf<String, ProxyBoneFlags>().apply {
				putAll(DEFAULT_FLAGS)
				putAll(bones)
			}
			return ProxyBoneConfigData(merged, timeline, transitionTicks)
		}
	}


	/** 根据当前动画时间合并基础配置和时间线配置 */
	fun resolveBoneFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val result = bones.toMutableMap()
		for (entry in timeline) {
			if (animTime >= entry.from && animTime < entry.to) {
				result.putAll(entry.bones)
			}
		}
		return result
	}

	/** 获取当前动画涉及的所有骨骼名（包括时间线中的） */
	fun resolveCurrentBoneNames(): Set<String> {
		val names = bones.keys.toMutableSet()
		for (entry in timeline) {
			names.addAll(entry.bones.keys)
		}
		return names
	}
}

