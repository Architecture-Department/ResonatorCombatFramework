package architecture.resonator_combat_framework.module.entity_animation.data

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.engine.BrBone

// 骨骼配置数据。包含过渡时间、骨骼标志、时间线
@ExposedCopyVisibility
@AllOpe
data class ProxyBoneConfigData
private constructor(
	val bones: Map<String, ProxyBoneFlags> = DEFAULT_FLAGS,
	val timeline: List<ProxyTimelineEntry> = emptyList(),
	val transitionTicks: Int = DEFAULT_TRANSITION_TICKS,
	private val fadeInTicks: Int = -1,
	private val fadeOutTicks: Int = -1,
	/** 额外骨骼定义（动画期间动态添加的骨骼） */
	val extraBones: Map<String, BrBone> = emptyMap()
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
			transitionTicks: Int,
			fadeInTicks: Int = -1,
			fadeOutTicks: Int = -1,
			/* 额外骨骼定义（JSON extra_bones 段） */
			extraBones: Map<String, BrBone> = emptyMap()
		): ProxyBoneConfigData {
			val merged = mutableMapOf<String, ProxyBoneFlags>().apply {
				putAll(DEFAULT_FLAGS)
				putAll(bones)
			}
			return ProxyBoneConfigData(merged, timeline, transitionTicks, fadeInTicks, fadeOutTicks, extraBones)
		}
	}

	/** 获取淡入 tick 数，未设置时退化为 transitionTicks */
	fun getFadeInTicks(): Int = if (fadeInTicks >= 0) fadeInTicks else transitionTicks

	/** 获取淡出 tick 数，未设置时退化为 transitionTicks */
	fun getFadeOutTicks(): Int = if (fadeOutTicks >= 0) fadeOutTicks else transitionTicks

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

