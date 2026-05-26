package architecture.resonator_combat_framework.module.player_animation.config

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.player_animation.init.ProxyBoneStateRegistry

@AllOpe
data class ProxyBoneFlags(
	val flags: Map<String, Boolean> = emptyMap()
) {
	val activeStates: Set<String> get() = flags.filter { it.value }.keys

	fun hasAnyLockState(): Boolean = activeStates.any { ProxyBoneStateRegistry.get(it).lockVanilla() }

	/** 是否参与 crossfade 过渡（默认 true），false 时该骨骼在跨动画时不混合 */
	fun shouldBlend(): Boolean = flags["blend"] != false

	/** 是否参与 blendFactor 淡入淡出（默认 true），false 时该骨骼不受 weight 影响 */
	fun shouldTransition(): Boolean = flags["transition"] != false

	fun isEnabled(axis: String): Boolean = flags[axis] != false
}