package architecture.resonator_combat_framework.module.player_animation.config

import architecture.resonator_combat_framework.module.player_animation.animdata.BoneStateRegistry

/// 在这注册骨骼状态
data class RcfBoneFlags(
	val flags: Map<String, Boolean> = emptyMap()
) {
	val lock: Boolean get() = flags["lock"] == true
	val noFadeIn: Boolean get() = flags["no_fade_in"] == true
	val noFadeOut: Boolean get() = flags["no_fade_out"] == true

	val activeStates: Set<String> get() = flags.filter { it.value }.keys

	fun hasAnyLockState(): Boolean = activeStates.any { BoneStateRegistry.isLockState(it) }
}

data class RcfTimelineEntry(
	val from: Float,
	val to: Float,
	val bones: Map<String, RcfBoneFlags>
)

data class RcfBoneConfig(
	val bones: Map<String, RcfBoneFlags> = emptyMap(),
	val timeline: List<RcfTimelineEntry> = emptyList()
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

	fun hasFlag(predicate: (RcfBoneFlags) -> Boolean): Boolean {
		if (bones.values.any(predicate)) return true
		return timeline.any { it.bones.values.any(predicate) }
	}

	fun hasNoFadeIn(): Boolean = hasFlag { it.noFadeIn }
	fun hasNoFadeOut(): Boolean = hasFlag { it.noFadeOut }

	fun allFlags(): Sequence<RcfBoneFlags> = bones.values.asSequence() +
		timeline.asSequence().flatMap { it.bones.values }

	fun resolveBlendSpeed(): Float {
		BoneStateRegistry.getBlendSpeedOverride(
			allFlags().flatMap { it.activeStates }.toSet()
		)?.let { return it }
		return 0.12f
	}

	companion object {
		val EMPTY = RcfBoneConfig()
	}
}
