package architecture.resonator_combat_framework.module.player_animation.config


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

	fun resolveBlendSpeed(): Float {
		for (flags in bones.values) {
			for (name in flags.activeStates) {
				BoneStateRegistry.get(name).blendSpeedOverride()?.let { return it }
			}
		}
		for (entry in timeline) {
			for (flags in entry.bones.values) {
				for (name in flags.activeStates) {
					BoneStateRegistry.get(name).blendSpeedOverride()?.let { return it }
				}
			}
		}
		return 0.12f
	}

	companion object {
		@JvmField
		val EMPTY = RcfBoneConfig()
	}
}



