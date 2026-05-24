package architecture.resonator_combat_framework.module.player_animation.config

import architecture.resonator_combat_framework.module.player_animation.init.ProxyBoneStateRegistry

data class ProxyBoneFlags(
	val flags: Map<String, Boolean> = emptyMap()
) {
	val activeStates: Set<String> get() = flags.filter { it.value }.keys

	fun hasAnyLockState(): Boolean = activeStates.any { ProxyBoneStateRegistry.get(it).lockVanilla() }

	fun isEnabled(axis: String): Boolean = flags[axis] != false
}