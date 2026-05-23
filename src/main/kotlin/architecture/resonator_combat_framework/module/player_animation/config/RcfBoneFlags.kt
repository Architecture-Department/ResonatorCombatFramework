package architecture.resonator_combat_framework.module.player_animation.config

data class RcfBoneFlags(
	val flags: Map<String, Boolean> = emptyMap()
) {
	val activeStates: Set<String> get() = flags.filter { it.value }.keys

	fun hasAnyLockState(): Boolean = activeStates.any { BoneStateRegistry.get(it).lockVanilla() }

	fun isEnabled(axis: String): Boolean = flags[axis] != false
}